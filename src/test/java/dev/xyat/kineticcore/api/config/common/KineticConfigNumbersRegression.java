package dev.xyat.kineticcore.api.config.common;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Headless verification that shared numeric decoding rejects precision loss. */
public final class KineticConfigNumbersRegression {
    private static int cases;

    public static void main(String[] args) {
        check(KineticConfigNumbers.exactInt(new BigDecimal("7.0000000000000001")) == null, "fractional int");
        check(KineticConfigNumbers.exactInt(new BigInteger("4294967297")) == null, "int overflow");
        check(KineticConfigNumbers.exactInt(new BigDecimal("42.000")) == 42, "exact decimal int");
        check(KineticConfigNumbers.exactLong(new BigDecimal("9007199254740993.000001")) == null, "fractional long");
        check(KineticConfigNumbers.exactLong(BigInteger.ONE.shiftLeft(100)) == null, "long overflow");
        check(KineticConfigNumbers.exactLong(new BigInteger("9007199254740993")) == 9007199254740993L, "exact large long");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("1.000000000000001"), 0, 1) == null, "decimal over maximum");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("-0.000000000000001"), 0, 1) == null, "decimal below minimum");
        check(KineticConfigNumbers.finiteDoubleInRange(new BigDecimal("0.25"), 0, 1) == 0.25, "valid decimal");
        System.out.println("PASS: " + cases + " shared numeric conversion regression cases");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        cases++;
    }
}
