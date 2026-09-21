package dev.xyat.kineticcore.api.config.server;

/** Ensures public numeric getters never silently truncate or saturate config values. */
public final class KTServerConfigGetterRegression {
    public static void main(String[] args) {
        oversizedLongMustNotWrapToInt();
        fractionalDoubleMustNotBecomeInteger();
        outOfRangeDoubleMustNotSaturateToLong();
        exactCrossTypeValuesRemainReadable();
        System.out.println("PASS: 4 server-config numeric getter regression cases");
    }

    private static void oversizedLongMustNotWrapToInt() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("test:large_value")
                .longValue("amount", () -> 4_294_967_297L, ignored -> {}, Long.MIN_VALUE, Long.MAX_VALUE)
                .build());
        check(KTServerConfigApi.getInt("test:large_value", "amount", 77) == 77,
                "oversized long must return caller's int fallback, not wrap to 1");
    }

    private static void fractionalDoubleMustNotBecomeInteger() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("test:fractional_value")
                .doubleValue("amount", () -> 2.5D, ignored -> {}, -100D, 100D)
                .build());
        check(KTServerConfigApi.getInt("test:fractional_value", "amount", 77) == 77,
                "fractional double must return int fallback");
        check(KTServerConfigApi.getLong("test:fractional_value", "amount", 88L) == 88L,
                "fractional double must return long fallback");
    }

    private static void outOfRangeDoubleMustNotSaturateToLong() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("test:out_of_range")
                .doubleValue("amount", () -> 0x1.0p63, ignored -> {}, 0D, Double.MAX_VALUE)
                .build());
        check(KTServerConfigApi.getLong("test:out_of_range", "amount", 88L) == 88L,
                "double 2^63 must return long fallback, not saturate to Long.MAX_VALUE");
    }

    private static void exactCrossTypeValuesRemainReadable() {
        KTServerConfigApi.register(KTServerConfigSpec.builder("test:exact_value")
                .longValue("longAmount", () -> 42L, ignored -> {}, 0, Long.MAX_VALUE)
                .doubleValue("doubleAmount", () -> 42D, ignored -> {}, 0D, 100D)
                .build());
        check(KTServerConfigApi.getInt("test:exact_value", "longAmount", -1) == 42,
                "in-range long is an exact int");
        check(KTServerConfigApi.getLong("test:exact_value", "doubleAmount", -1L) == 42L,
                "whole in-range double is an exact long");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
