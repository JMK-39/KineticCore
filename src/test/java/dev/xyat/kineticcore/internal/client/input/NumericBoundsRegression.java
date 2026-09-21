package dev.xyat.kineticcore.internal.client.input;
import java.math.BigInteger;
import java.math.BigDecimal;

/** Validates numeric field bounds without narrowing or rounding caller-specified numbers. */
public final class NumericBoundsRegression {
    private static int cases;
    private static void check(boolean condition, String message) {
        cases++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(NumericInputRules.integerValue("5", 4_294_967_301L, null, null) == null,
                "long lower limit must not truncate into int range");
        check(Integer.valueOf(5).equals(NumericInputRules.integerValue("5", null, 4_294_967_296L, null)),
                "long upper limit above int range must not become zero");
        check(NumericInputRules.integerValue("5", new BigDecimal("5.5"), null, null) == null,
                "fractional lower bound must not be truncated");
        check(NumericInputRules.longValue("5", new BigInteger("18446744073709551617"), null, null) == null,
                "BigInteger lower bound must not wrap into long");
        check(Long.valueOf(5).equals(NumericInputRules.longValue("5", null, new BigInteger("18446744073709551617"), null)),
                "BigInteger upper bound must not wrap into long");
        check(NumericInputRules.longValue("5", new BigDecimal("5.5"), null, null) == null,
                "fractional long lower bound must not truncate");
        check(NumericInputRules.doubleValue("0.1", new BigDecimal("0.100000000000000000000001"), null, null) == null,
                "high precision decimal lower bound must not be rounded down");
        check(NumericInputRules.doubleValue("0.1", null, new BigDecimal("0.099999999999999999999999"), null) == null,
                "high precision decimal upper bound must not be rounded up");
        check(Double.valueOf(0.1).equals(NumericInputRules.doubleValue("0.1", 0.1, 0.1, null)),
                "equal double bounds must still accept valid values");
        check(NumericInputRules.integerValue("5", 1, 10, ignored -> false) == null,
                "business validator must still reject inputs");
        System.out.println("PASS: " + cases + " numeric boundary regression cases");
    }
}
