package dev.xyat.kineticcore.api.config.common;

import java.math.BigDecimal;

/** Bounds must be checked against the exact value which is actually returned. */
public final class KineticConfigNumberConsistencyRegression {
    private static int checks;

    public static void main(String[] args) {
        Number inconsistent = new Number() {
            public int intValue() { return 50; }
            public long longValue() { return 50; }
            public float floatValue() { return 50; }
            public double doubleValue() { return 50; }
            public String toString() { return "5"; }
        };
        Double value = KineticConfigNumbers.finiteDoubleInRange(inconsistent, 0, 10);
        check(value != null && value == 5.0D, "returned double must match checked decimal");
        Number inverse = new Number() {
            public int intValue() { return 1; }
            public long longValue() { return 1; }
            public float floatValue() { return 1; }
            public double doubleValue() { return 1; }
            public String toString() { return "100"; }
        };
        check(KineticConfigNumbers.finiteDoubleInRange(inverse, 0, 10) == null,
                "rejected precise value must not pass using its rounded double");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("0.25"), 0, 1) == 0.25D,
                "normal BigDecimal conversion must remain intact");
        System.out.println("PASS: " + checks + " numeric-consistency checks");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
