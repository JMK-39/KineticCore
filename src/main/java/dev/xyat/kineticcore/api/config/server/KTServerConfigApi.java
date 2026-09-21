package dev.xyat.kineticcore.api.config.server;

import dev.xyat.kineticcore.internal.config.ServerConfigNetwork;
import dev.xyat.kineticcore.api.config.common.KineticConfigNumbers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Public API type for kt server config api. */
public final class KTServerConfigApi {
    private static final Map<String, KTServerConfigSpec> SPECS = new LinkedHashMap<>();

    private KTServerConfigApi() {
    }

    /**
     * Registers this API capability.
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
     * Registers action page.
     */
    public static synchronized void registerActionPage(String pageId) {
        if (SPECS.containsKey(pageId)) return;
        register(KTServerConfigSpec.builder(pageId).build());
    }

    /**
     * Performs the find API operation.
     */
    public static synchronized Optional<KTServerConfigSpec> find(String pageId) {
        return Optional.ofNullable(SPECS.get(pageId));
    }

    /**
     * Returns whether registered.
     */
    public static synchronized boolean isRegistered(String pageId) {
        return SPECS.containsKey(pageId);
    }

    /**
     * Returns boolean.
     */
    public static boolean getBoolean(String pageId, String entryId, boolean fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }

    /** 读取整数；超过 int 范围或包含小数时返回 fallback，不截断配置值。 */
    public static int getInt(String pageId, String entryId, int fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        Integer converted = KineticConfigNumbers.exactInt(number);
        return converted == null ? fallback : converted;
    }

    /** 读取长整数；超出 long 范围或包含小数时返回 fallback，不执行饱和转换。 */
    public static long getLong(String pageId, String entryId, long fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        Long converted = KineticConfigNumbers.exactLong(number);
        return converted == null ? fallback : converted;
    }

    /** 读取有限小数；无法转换为有限 double 时返回 fallback。 */
    public static double getDouble(String pageId, String entryId, double fallback) {
        Object value = valueOrNull(pageId, entryId);
        if (!(value instanceof Number number)) return fallback;
        double decoded = number.doubleValue();
        return Double.isFinite(decoded) ? decoded : fallback;
    }

    /**
     * Returns string.
     */
    public static String getString(String pageId, String entryId, String fallback) {
        Object value = valueOrNull(pageId, entryId);
        return value instanceof String stringValue ? stringValue : fallback;
    }

    /**
     * Returns string list.
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
