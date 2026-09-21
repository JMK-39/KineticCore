package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigEntry;
import dev.xyat.kineticcore.api.config.client.KTConfigPage;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Check numeric normalization before applying authoritative server values locally. */
public final class ServerConfigMirrorPrecisionRegression {
    public static void main(String[] args) throws Exception {
        Method normalize = ServerConfigClientRuntime.class.getDeclaredMethod("normalizeForEntry", KTConfigEntry.class, Object.class);
        normalize.setAccessible(true);
        KTConfigEntry<?> doubleEntry = KTConfigPage.builder("test:mirror", Component.empty())
                .doubleValue("value", Component.empty(), () -> 0.25, ignored -> {}, 0.25, 0.0, 1.0, null)
                .build().entries().get(0);
        check(normalize.invoke(null, doubleEntry, new BigDecimal("1.00000000000000001")) == null,
                "out-of-range decimal was rounded into mirror's valid range");
        check(normalize.invoke(null, doubleEntry, new BigDecimal("0.25")).equals(0.25),
                "valid decimal should still sync");
        Method intDecode = ServerConfigClientRuntime.class.getDeclaredMethod("safeInt", Number.class);
        intDecode.setAccessible(true);
        check(intDecode.invoke(null, new BigDecimal("10.00000000000000001")) == null,
                "fractional int was accepted after rounding");
        Method longDecode = ServerConfigClientRuntime.class.getDeclaredMethod("safeLong", Number.class);
        longDecode.setAccessible(true);
        check(longDecode.invoke(null, new BigDecimal("9007199254740993.0000000001")) == null,
                "fractional long was accepted after rounding");
        check(longDecode.invoke(null, new BigInteger("9007199254740993")).equals(9007199254740993L),
                "exact large integer must remain valid");
        System.out.println("PASS: 5 client mirror precision regression cases");
    }
    private static void check(boolean ok, String message) {if (!ok) throw new AssertionError(message);}
}
