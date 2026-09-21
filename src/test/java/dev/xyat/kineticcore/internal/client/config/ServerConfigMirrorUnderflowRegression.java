package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigEntry;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;

/** Regression for decimal underflow in the shared client-side authoritative mirror. */
public final class ServerConfigMirrorUnderflowRegression {
    private static int cases;

    public static void main(String[] args) throws Exception {
        Method normalize = ServerConfigClientRuntime.class.getDeclaredMethod(
                "normalizeForEntry", KTConfigEntry.class, Object.class);
        normalize.setAccessible(true);
        KTConfigEntry<?> entry = KTConfigPage.builder("test:mirror", Component.empty())
                .doubleValue("value", Component.empty(), () -> 0.25, ignored -> {}, 0.25, 0.0, 1.0, null)
                .build().entries().get(0);
        check(normalize.invoke(null, entry, new BigDecimal("1e-1000")) == null,
                "mirror must not silently erase a nonzero decimal");
        check(Double.valueOf(0.25).equals(normalize.invoke(null, entry, new BigDecimal("0.25"))),
                "ordinary mirror values remain usable");

        Class<?> stateClass = Class.forName(ServerConfigClientRuntime.class.getName() + "$State");
        Constructor<?> constructor = stateClass.getDeclaredConstructor(
                boolean.class, boolean.class, Map.class, long.class, String.class);
        constructor.setAccessible(true);
        Field stateMapField = ServerConfigClientRuntime.class.getDeclaredField("STATES");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateMapField.get(null);
        String id = "test:underflow_getter";
        try {
            states.put(id, constructor.newInstance(true, true,
                    Map.of("tiny", new BigDecimal("1e-1000"), "valid", new BigDecimal("0.25")), 1L, ""));
            check(ServerConfigClientRuntime.getDouble(id, "tiny", -7.0) == -7.0,
                    "unrepresentable nonzero value must use getter fallback");
            check(ServerConfigClientRuntime.getDouble(id, "valid", -7.0) == 0.25,
                    "finite in-range value remains readable");
        } finally {
            states.remove(id);
        }
        System.out.println("PASS: " + cases + " client mirror underflow cases");
    }

    private static void check(boolean ok, String reason) {
        cases++;
        if (!ok) throw new AssertionError(reason);
    }
}
