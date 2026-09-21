package dev.xyat.kineticcore.api.config.common;

import java.math.BigDecimal;

/**
 * 为核心及附属提供无截断的配置数字转换；返回 null 表示输入无法精确表示。
 * 不允许通过先转换为 double 的方式判断高精度整数是否有效。
 */
public final class KineticConfigNumbers {
    private KineticConfigNumbers() {
    }

    /** Converts a finite, exactly integral number to int, rejecting fractions and overflow. */
    public static Integer exactInt(Number number) {
        if (number == null) return null;
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException invalid) {
            return null;
        }
    }

    /** Converts a finite, exactly integral number to long, rejecting fractions and overflow. */
    public static Long exactLong(Number number) {
        if (number == null) return null;
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException invalid) {
            return null;
        }
    }

    /** Checks decimal bounds before any conversion to double can silently round a value into range. */
    public static Double finiteDoubleInRange(Number number, double minimum, double maximum) {
        if (number == null || !Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) return null;
        try {
            BigDecimal decimal = new BigDecimal(number.toString());
            if (decimal.compareTo(BigDecimal.valueOf(minimum)) < 0
                    || decimal.compareTo(BigDecimal.valueOf(maximum)) > 0) return null;
            // Convert the precise decimal that passed the bounds check, not an unrelated
            // Number.doubleValue() implementation which may return a different value.
            double converted = decimal.doubleValue();
            if (!Double.isFinite(converted) || (decimal.signum() != 0 && converted == 0.0D)) return null;
            return converted;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }
}
