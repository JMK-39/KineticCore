package dev.xyat.kineticcore.api.config.server;

import dev.xyat.kineticcore.internal.config.ServerConfigNetwork;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class KTServerConfigApi {
    private static final Map<String, KTServerConfigSpec> SPECS = new LinkedHashMap<>();
    private static boolean networkInitialized;

    private KTServerConfigApi() {
    }

    public static synchronized void register(KTServerConfigSpec spec) {
        Objects.requireNonNull(spec, "spec");
        ensureNetwork();
        KTServerConfigSpec old = SPECS.putIfAbsent(spec.pageId(), spec);
        if (old != null && old != spec) {
            throw new IllegalStateException("Server config page is already registered: " + spec.pageId());
        }
    }

    public static synchronized void registerActionPage(String pageId) {
        if (SPECS.containsKey(pageId)) return;
        register(KTServerConfigSpec.builder(pageId).build());
    }

    public static synchronized Optional<KTServerConfigSpec> find(String pageId) {
        return Optional.ofNullable(SPECS.get(pageId));
    }

    public static synchronized boolean isRegistered(String pageId) {
        return SPECS.containsKey(pageId);
    }

    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    public static int getInt(String pageId, String entryId, int fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    public static long getLong(String pageId, String entryId, long fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    public static double getDouble(String pageId, String entryId, double fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    public static String getString(String pageId, String entryId, String fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof String stringValue ? stringValue : fallback;
    }

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
        if (networkInitialized) return;
        networkInitialized = true;
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
