package dev.xyat.kineticcore.api.config.common;

import java.math.BigDecimal;

/** Regression: shared decimal conversion must not erase nonzero values. */
public final class KineticConfigDecimalPrecisionRegression {
    private static int cases;

    public static void main(String[] args) {
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("1e-1000"), 0, 1) == null,
                "positive nonzero must not underflow to zero");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("-1e-1000"), -1, 1) == null,
                "negative nonzero must not underflow to negative zero");
        check(KineticConfigNumbers.finiteDoubleInRange(BigDecimal.ZERO, 0, 1) == 0.0,
                "exact zero is legal");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("1.0000000000000001"), 0, 1) == null,
                "out of bound raw fraction cannot round in");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("0.1"), 0, 1) == 0.1,
                "normal decimals remain supported");
        System.out.println("PASS: " + cases + " shared decimal precision cases");
    }

    private static void check(boolean ok, String reason) {
        cases++;
        if (!ok) throw new AssertionError(reason);
    }
}
