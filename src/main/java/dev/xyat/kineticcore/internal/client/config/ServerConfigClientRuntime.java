package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.config.client.KTConfigEntry;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import dev.xyat.kineticcore.api.config.client.KTConfigScope;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumbers;
import dev.xyat.kineticcore.internal.config.ServerConfigNetwork;
import dev.xyat.kineticcore.internal.client.KineticClientEventRuntime;

import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@OnlyIn(Dist.CLIENT)
public final class ServerConfigClientRuntime {
    private static boolean initialized;

    public static synchronized void initialize() {
        if (initialized) return;
        // Install both listeners as one operation. If the second registration fails,
        // release the first handle so retrying does not accumulate duplicate callbacks.
        var loginSubscription = KineticClientEventRuntime.registerLogin(ServerConfigClientRuntime::clear);
        try {
            KineticClientEventRuntime.registerLogout(ServerConfigClientRuntime::clear);
            initialized = true;
        } catch (RuntimeException | Error failure) {
            try {
                loginSubscription.close();
            } catch (RuntimeException | Error cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private record State(
            boolean loaded,
            boolean editable,
            Map<String, Object> values,
            long revision,
            String loadFailureKey
    ) {
    }

    private static final Map<String, State> STATES = new HashMap<>();
    private static long revisionCounter;

    private ServerConfigClientRuntime() {
    }

    public static boolean isLoaded(String pageId) {
        State state = STATES.get(pageId);
        return state != null && state.loaded;
    }

    public static boolean canEdit(String pageId) {
        State state = STATES.get(pageId);
        return state != null && state.loaded && state.editable;
    }

    public static long revision(String pageId) {
        State state = STATES.get(pageId);
        return state == null ? 0L : state.revision;
    }

    public static String loadFailureKey(String pageId) {
        State state = STATES.get(pageId);
        return state == null || state.loadFailureKey == null ? "" : state.loadFailureKey;
    }

    public static void request(KTConfigPage page) {
        Objects.requireNonNull(page, "page");
        if (page.scope() != KTConfigScope.SERVER_AUTHORITATIVE || !page.serverManaged()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) return;
        markLoading(page.id());
        try {
            ServerConfigNetwork.requestPage(page.id());
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to request server config page {}", page.id(), throwable);
            markLoadFailure(page.id(), "gui.kineticcore.config.server.load_failed");
            refreshOpenScreen(page.id());
        }
    }

    public static void request(String pageId) {
        KTConfigApi.find(pageId).ifPresent(ServerConfigClientRuntime::request);
    }

    public static boolean save(KTConfigPage page, Map<String, Object> changedValues) {
        Objects.requireNonNull(page, "page");
        if (!page.serverManaged() || !canEdit(page.id())) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || minecraft.player == null) return false;
        try {
            byte[] payload = ServerConfigNetwork.encodeValues(changedValues);
            ServerConfigNetwork.savePage(page.id(), payload);
            return true;
        } catch (Throwable throwable) {
            KineticRuntime.logger().error("Failed to send server config page {}", page.id(), throwable);
            KineticOverlays.toast("kineticcore_server_config_save_failed", Component.translatable("gui.kineticcore.config.server.save_failed"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
            return false;
        }
    }

    public static boolean savePartial(String pageId, Map<String, Object> changedValues) {
        KTConfigPage page = KTConfigApi.find(pageId).orElse(null);
        return page != null && save(page, changedValues);
    }

    public static void applyCached(KTConfigPage page) {
        State state = STATES.get(page.id());
        if (state != null && state.loaded) {
            applyToClientMirror(page, state.values);
        }
    }

    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        Object raw = value(pageId, entryId);
        return raw instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    public static int getInt(String pageId, String entryId, int fallback) {
        Object raw = value(pageId, entryId);
        if (!(raw instanceof Number number)) return fallback;
        Integer decoded = safeInt(number);
        return decoded == null ? fallback : decoded;
    }

    public static long getLong(String pageId, String entryId, long fallback) {
        Object raw = value(pageId, entryId);
        if (!(raw instanceof Number number)) return fallback;
        Long decoded = safeLong(number);
        return decoded == null ? fallback : decoded;
    }

    public static double getDouble(String pageId, String entryId, double fallback) {
        Object raw = value(pageId, entryId);
        if (!(raw instanceof Number number)) return fallback;
        Double decoded = KineticConfigNumbers.finiteDoubleInRange(number, -Double.MAX_VALUE, Double.MAX_VALUE);
        return decoded == null ? fallback : decoded;
    }

    public static String getString(String pageId, String entryId, String fallback) {
        Object value = value(pageId, entryId);
        return value instanceof String string ? string : fallback;
    }

    public static List<String> getStringList(String pageId, String entryId, List<String> fallback) {
        Object raw = value(pageId, entryId);
        if (!(raw instanceof List<?> list)) return List.copyOf(fallback);
        List<String> result = new ArrayList<>(list.size());
        for (Object value : list) {
            if (!(value instanceof String string)) return List.copyOf(fallback);
            result.add(string);
        }
        return List.copyOf(result);
    }


    public static List<Integer> getIntegerList(String pageId, String entryId, List<Integer> fallback) {
        Object raw = value(pageId, entryId);
        List<Integer> result = normalizeIntegerList(raw);
        return result == null ? List.copyOf(fallback) : List.copyOf(result);
    }

    public static void handleSync(
            String pageId,
            boolean editable,
            boolean saveResponse,
            boolean success,
            String messageKey,
            byte[] payload
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        boolean snapshotLoaded = false;
        String loadFailureKey = "";
        if (payload != null && payload.length > 0) {
            try {
                // Validate the entire decoded snapshot before publishing any of its values.
                // Map.copyOf also rejects null keys/values from malformed network payloads.
                values.putAll(Map.copyOf(ServerConfigNetwork.decodeValues(payload)));
                snapshotLoaded = true;
            } catch (Throwable throwable) {
                KineticRuntime.logger().error("Failed to decode server config snapshot {}", pageId, throwable);
                success = false;
                messageKey = "gui.kineticcore.config.server.load_failed";
                loadFailureKey = messageKey;
            }
        } else if (!success) {
            loadFailureKey = messageKey == null || messageKey.isBlank()
                    ? "gui.kineticcore.config.server.load_failed"
                    : messageKey;
        } else {
            success = false;
            messageKey = "gui.kineticcore.config.server.load_failed";
            loadFailureKey = messageKey;
        }

        long revision = ++revisionCounter;
        STATES.put(pageId, new State(
                snapshotLoaded,
                snapshotLoaded && editable,
                Map.copyOf(values),
                revision,
                snapshotLoaded ? "" : loadFailureKey
        ));
        if (snapshotLoaded) {
            KTConfigApi.find(pageId).ifPresent(page -> applyToClientMirror(page, values));
        }
        refreshOpenScreen(pageId);

        if (saveResponse) {
            if (success) {
                KTConfigPage page = KTConfigApi.find(pageId).orElse(null);
                if (page != null) {
                    KTConfigApi.notifySaved(page);
                } else {
                    KineticOverlays.toast("kineticcore_server_config_saved:" + pageId, Component.translatable("gui.kineticcore.config.server.saved"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
                }
            } else {
                Component message = Component.translatable(
                        messageKey == null || messageKey.isBlank()
                                ? "gui.kineticcore.config.server.save_failed"
                                : messageKey
                );
                KineticOverlays.toast("kineticcore_server_config_save_failed:" + pageId, message, KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
            }
        }
    }

    private static void markLoading(String pageId) {
        long revision = ++revisionCounter;
        STATES.put(pageId, new State(false, false, Map.of(), revision, ""));
    }

    private static void markLoadFailure(String pageId, String messageKey) {
        long revision = ++revisionCounter;
        STATES.put(pageId, new State(false, false, Map.of(), revision, messageKey == null ? "" : messageKey));
    }

    private static Object value(String pageId, String entryId) {
        State state = STATES.get(pageId);
        return state == null ? null : state.values.get(entryId);
    }

    private static void applyToClientMirror(KTConfigPage page, Map<String, Object> values) {
        for (KTConfigEntry<?> entry : page.entries()) {
            if (!entry.isValueEntry() || !values.containsKey(entry.id())) continue;
            try {
                Object normalized = normalizeForEntry(entry, values.get(entry.id()));
                if (normalized == null) continue;
                // The entry owns validation and rollback: do not invoke a stateful
                // addon validator once here and again during the actual write.
                entry.applySnapshotWithRollback(normalized);
            } catch (Throwable throwable) {
                KineticRuntime.logger().error(
                        "Failed to apply server config mirror {}/{}",
                        page.id(),
                        entry.id(),
                        throwable
                );
            }
        }
    }

    private static Object normalizeForEntry(KTConfigEntry<?> entry, Object raw) {
        if (raw == null) return null;
        return switch (entry.type()) {
            case INTEGER, COLOR -> raw instanceof Number number ? safeInt(number) : raw;
            case LONG -> raw instanceof Number number ? safeLong(number) : raw;
            case DOUBLE -> {
                if (!(raw instanceof Number number)) yield raw;
                // Keep the nullable result boxed; mixing Double and primitive double in
                // a conditional expression would unbox null and throw during rejection.
                if (entry.minimum() != null && entry.maximum() != null) {
                    yield KineticConfigNumbers.finiteDoubleInRange(
                            number, entry.minimum().doubleValue(), entry.maximum().doubleValue());
                }
                yield KineticConfigNumbers.finiteDoubleInRange(number, -Double.MAX_VALUE, Double.MAX_VALUE);
            }
            case INTEGER_LIST -> normalizeIntegerList(raw);
            default -> raw;
        };
    }

    private static Integer safeInt(Number number) {
        return KineticConfigNumbers.exactInt(number);
    }

    private static Long safeLong(Number number) {
        return KineticConfigNumbers.exactLong(number);
    }

    private static List<Integer> normalizeIntegerList(Object raw) {
        if (!(raw instanceof List<?> list)) return null;
        List<Integer> result = new ArrayList<>(list.size());
        for (Object value : list) {
            if (!(value instanceof Number number)) return null;
            Integer decoded = safeInt(number);
            if (decoded == null) return null;
            result.add(decoded);
        }
        return result;
    }

    private static void refreshOpenScreen(String pageId) {
        Screen screen = Minecraft.getInstance().screen;
        ConfigScreens.serverSnapshotUpdated(screen, pageId);
    }

    private static void clear() {
        STATES.clear();
        revisionCounter++;
    }
}
