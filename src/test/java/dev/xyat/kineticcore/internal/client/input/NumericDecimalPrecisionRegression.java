package dev.xyat.kineticcore.internal.client.input;

import java.math.BigDecimal;

/** Regression: compare typed decimal text before any double rounding occurs. */
public final class NumericDecimalPrecisionRegression {
    private static int cases;

    public static void main(String[] args) {
        check(NumericInputRules.doubleValue("1.0000000000000001", 0, 1, null) == null,
                "fraction above maximum must not round down into range");
        check(NumericInputRules.doubleValue("-0.0000000000000001", 0, 1, null) == null,
                "fraction below minimum must not round up into range");
        check(NumericInputRules.doubleValue("0.99999999999999999", 1, 2, null) == null,
                "fraction below minimum must not round up to one");
        check(NumericInputRules.doubleValue("1.0000000000000001", new BigDecimal("1.00000000000000005"), 2, null) != null,
                "exactly in-range high precision decimal remains supported");
        String underflow = "0." + "0".repeat(400) + "1";
        check(NumericInputRules.doubleValue(underflow, 0, 1, null) == null,
                "nonzero sub-double value must not silently become zero");
        check(Double.valueOf(0.0).equals(NumericInputRules.doubleValue("0", 0, 1, null)),
                "actual zero remains accepted");
        check(Double.valueOf(0.1).equals(NumericInputRules.doubleValue("0.1", 0, 1, null)),
                "normal decimal remains accepted");
        System.out.println("PASS: " + cases + " numeric text precision cases");
    }

    private static void check(boolean ok, String reason) {
        cases++;
        if (!ok) throw new AssertionError(reason);
    }
}
