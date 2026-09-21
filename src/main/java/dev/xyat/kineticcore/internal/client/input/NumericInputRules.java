package dev.xyat.kineticcore.internal.client.input;

import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields;

import java.math.BigDecimal;
import java.util.function.Predicate;

public final class NumericInputRules {
    private NumericInputRules() {
    }

    public static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static Integer integerValue(
            String raw,
            Number minValue,
            Number maxValue,
            Predicate<Number> validator
    ) {
        if (raw == null || raw.isEmpty() || raw.equals("-")) return null;
        try {
            int value = Integer.parseInt(raw);
            if (outsideBounds(BigDecimal.valueOf(value), minValue, maxValue)) return null;
            Integer boxed = value;
            return validator == null || validator.test(boxed) ? boxed : null;
        } catch (RuntimeException invalid) {
            // A failed add-on validator or malformed caller bound makes the input invalid;
            // it must not abort rendering the shared numeric field.
            return null;
        }
    }

    public static Long longValue(
            String raw,
            Number minValue,
            Number maxValue,
            Predicate<Number> validator
    ) {
        if (raw == null || raw.isEmpty() || raw.equals("-")) return null;
        try {
            long value = Long.parseLong(raw);
            if (outsideBounds(BigDecimal.valueOf(value), minValue, maxValue)) return null;
            Long boxed = value;
            return validator == null || validator.test(boxed) ? boxed : null;
        } catch (RuntimeException invalid) {
            // A failed add-on validator or malformed caller bound makes the input invalid;
            // it must not abort rendering the shared numeric field.
            return null;
        }
    }

    public static Double doubleValue(
            String raw,
            Number minValue,
            Number maxValue,
            Predicate<Number> validator
    ) {
        if (raw == null || raw.isEmpty() || raw.equals("-") || raw.equals(".") || raw.equals("-.")) return null;
        try {
            // Validate the typed decimal before converting it to a double. Conversion can
            // round an out-of-range value onto a boundary, or erase a nonzero value.
            BigDecimal decimal = new BigDecimal(raw);
            double value = decimal.doubleValue();
            if (!Double.isFinite(value) || (decimal.signum() != 0 && value == 0.0D)) return null;
            if (outsideBounds(decimal, minValue, maxValue)) return null;
            Double boxed = value;
            return validator == null || validator.test(boxed) ? boxed : null;
        } catch (RuntimeException invalid) {
            // A failed add-on validator or malformed caller bound makes the input invalid;
            // it must not abort rendering the shared numeric field.
            return null;
        }
    }

    /** Compare numeric limits without narrowing large integers or rounding precise decimal bounds. */
    private static boolean outsideBounds(BigDecimal value, Number minimum, Number maximum) {
        try {
            return (minimum != null && value.compareTo(new BigDecimal(minimum.toString())) < 0)
                    || (maximum != null && value.compareTo(new BigDecimal(maximum.toString())) > 0);
        } catch (NumberFormatException invalidLimit) {
            // An invalid caller-supplied bound cannot safely permit a value.
            return true;
        }
    }

    public static boolean isAllowedText(
            String raw,
            KineticNumericFields.Type type,
            boolean allowNegative
    ) {
        if (raw == null) return false;
        if (raw.isEmpty()) return true;
        if (raw.charAt(0) == '-' && !allowNegative) return false;

        return switch (type) {
            case INTEGER, LONG -> raw.matches(allowNegative ? "-?\\d*" : "\\d*");
            case DECIMAL -> raw.matches(allowNegative ? "-?(?:\\d+(?:\\.\\d*)?|\\.\\d*)" : "(?:\\d+(?:\\.\\d*)?|\\.\\d*)");
        };
    }
}
