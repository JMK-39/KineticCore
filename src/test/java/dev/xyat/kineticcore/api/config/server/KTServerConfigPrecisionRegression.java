package dev.xyat.kineticcore.api.config.server;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/** Regression for exact network-number decoding before mutating server config. */
public final class KTServerConfigPrecisionRegression {
    private static int count;

    public static void main(String[] args) {
        rejectsRoundedFractionalInt();
        rejectsRoundedFractionalLong();
        rejectsFractionalIntegerList();
        rejectsRoundedDoubleOutsideRange();
        preservesExactIntegerInputs();
        rejectsHugeIntegerWithoutSaturation();
        System.out.println("PASS: " + count + " exact server-number regression cases");
    }

    private static void rejectsRoundedFractionalInt() {
        int[] value = {7};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:precise_int")
                .intValue("value", () -> value[0], next -> value[0] = next, 0, 100).build();
        reject(() -> spec.apply(Map.of("value", new BigDecimal("10.0000000000000001"))));
        check(value[0] == 7, "fractional int was silently rounded and written");
    }

    private static void rejectsRoundedFractionalLong() {
        long[] value = {7};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:precise_long")
                .longValue("value", () -> value[0], next -> value[0] = next, 0, Long.MAX_VALUE).build();
        reject(() -> spec.apply(Map.of("value", new BigDecimal("9007199254740993.0000000001"))));
        check(value[0] == 7, "fractional long was silently rounded and written");
    }

    private static void rejectsFractionalIntegerList() {
        @SuppressWarnings("unchecked") List<Integer>[] values = new List[]{List.of(7)};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:precise_list")
                .intList("value", () -> values[0], next -> values[0] = next).build();
        reject(() -> spec.apply(Map.of("value", List.of(new BigDecimal("10.0000000000000001")))));
        check(values[0].equals(List.of(7)), "fractional list member was silently rounded");
    }

    private static void rejectsRoundedDoubleOutsideRange() {
        double[] value = {0.5};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:precise_double")
                .doubleValue("value", () -> value[0], next -> value[0] = next, 0, 1).build();
        reject(() -> spec.apply(Map.of("value", new BigDecimal("1.00000000000000001"))));
        check(value[0] == 0.5, "out-of-range decimal was rounded into double range");
    }

    private static void preservesExactIntegerInputs() {
        int[] intValue = {0}; long[] longValue = {0};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:exact_values")
                .intValue("integer", () -> intValue[0], next -> intValue[0] = next, 0, 100)
                .longValue("long", () -> longValue[0], next -> longValue[0] = next, Long.MIN_VALUE, Long.MAX_VALUE)
                .build();
        spec.apply(Map.of("integer", new BigDecimal("42.000"), "long", new BigInteger("9007199254740993")));
        check(intValue[0] == 42 && longValue[0] == 9007199254740993L,
                "exactly integral decimals and large exact integers must remain accepted");
        count++;
    }

    private static void rejectsHugeIntegerWithoutSaturation() {
        long[] value = {7};
        KTServerConfigSpec spec = KTServerConfigSpec.builder("test:huge_integer")
                .longValue("value", () -> value[0], next -> value[0] = next, Long.MIN_VALUE, Long.MAX_VALUE).build();
        reject(() -> spec.apply(Map.of("value", BigInteger.ONE.shiftLeft(120))));
        check(value[0] == 7, "out-of-range integer must not saturate or wrap");
    }

    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("expected invalid numeric input to be rejected"); }
        catch (IllegalArgumentException expected) { count++; }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
