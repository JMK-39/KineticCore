package dev.xyat.kineticcore.api.config.server;

import dev.xyat.kineticcore.internal.config.ServerConfigNetwork;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumbers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry of server-side config specs and typed read helpers for server code.
 *
 * <p>Registration also sets up the network channel that lets operators load and save pages from the client. All
 * methods are thread-safe.
 */
public final class KTServerConfigApi {
    private static final Map<String, KTServerConfigSpec> SPECS = new LinkedHashMap<>();

    private KTServerConfigApi() {
    }

    /**
     * Registers a server config spec. Call it during common setup, before players can open the page. Registering
     * the same instance again does nothing.
     *
     * @param spec spec to register
     * @throws NullPointerException if {@code spec} is {@code null}
     * @throws IllegalStateException if a different spec is already registered for the same page id
     */
    public static synchronized void register(KTServerConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        ensureNetwork();
        KTServerConfigSpec old = SPECS.putIfAbsent(spec.pageId(), spec);
        if (old != null && old != spec) {
            throw new IllegalStateException("Server config page is already registered: " + spec.pageId());
        }
    }

    /**
     * Registers an empty spec for a server page that only has action rows, so clients can still load its
     * permissions. Does nothing if the page is already registered.
     *
     * @param pageId namespaced page id
     * @throws IllegalArgumentException if {@code pageId} has no namespace
     */
    public static synchronized void registerActionPage(String pageId) {
        if (SPECS.containsKey(pageId)) return;
        register(KTServerConfigSpec.builder(pageId).build());
    }

    /**
     * Looks up a registered spec.
     *
     * @param pageId namespaced page id
     * @return the spec, or empty when none is registered
     */
    public static synchronized Optional<KTServerConfigSpec> find(String pageId) {
        return Optional.ofNullable(SPECS.get(pageId));
    }

    /** Returns whether a spec is registered for the page id. */
    public static synchronized boolean isRegistered(String pageId) {
        return SPECS.containsKey(pageId);
    }

    /**
     * Reads a boolean entry.
     *
     * @param pageId namespaced page id
     * @param entryId entry id
     * @param fallback value returned when the page or entry is missing or not a boolean
     * @return the current value or {@code fallback}
     */
    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    /**
     * 读取整数；超过 int 范围或包含小数时返回 fallback，不截断配置值。
     *
     * <p>Reads an integer entry. Returns {@code fallback} when the value is missing, fractional or outside the
     * {@code int} range; values are never truncated.
     */
    public static int getInt(String pageId, String entryId, int fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        Integer converted = KineticConfigNumbers.exactInt(number);
        return converted == null ? fallback : converted;
    }

    /**
     * 读取长整数；超出 long 范围或包含小数时返回 fallback，不执行饱和转换。
     *
     * <p>Reads a long entry. Returns {@code fallback} when the value is missing, fractional or outside the
     * {@code long} range; values are never saturated.
     */
    public static long getLong(String pageId, String entryId, long fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        Long converted = KineticConfigNumbers.exactLong(number);
        return converted == null ? fallback : converted;
    }

    /**
     * 读取有限小数；无法转换为有限 double 时返回 fallback。
     *
     * <p>Reads a decimal entry. Returns {@code fallback} when the value is missing or not a finite {@code double}.
     */
    public static double getDouble(String pageId, String entryId, double fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        double decoded = number.doubleValue();
        return Double.isFinite(decoded) ? decoded : fallback;
    }

    /**
     * Reads a text entry.
     *
     * @param pageId namespaced page id
     * @param entryId entry id
     * @param fallback value returned when the page or entry is missing or not a string; may be {@code null}
     * @return the current value or {@code fallback}
     */
    public static String getString(String pageId, String entryId, String fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof String stringValue ? stringValue : fallback;
    }

    /**
     * Reads a string list entry.
     *
     * @param pageId namespaced page id
     * @param entryId entry id
     * @param fallback list returned when the page or entry is missing or holds a non-string element; must not be
     *   {@code null}
     * @return an unmodifiable copy of the current value or of {@code fallback}
     */
    public static java.util.List<String> getStringList(String pageId, String entryId, java.util.List<String> fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof java.util.List<?> list)) return java.util.List.copyOf(fallback);
        java.util.ArrayList<String> result = new java.util.ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof String stringValue)) return java.util.List.copyOf(fallback);
            result.add(stringValue);
        }
        return java.util.List.copyOf(result);
    }

    private static void ensureNetwork() {
        ServerConfigNetwork.register();
    }

    private static Object valueOrNull(String pageId, String entryId) {
        KTServerConfigSpec spec;
        synchronized (KTServerConfigApi.class) {
            spec = SPECS.get(pageId);
        }
        if (spec == null) return null;
        try {
            return spec.value(entryId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
