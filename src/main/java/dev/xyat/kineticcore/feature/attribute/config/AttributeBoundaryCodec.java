package dev.xyat.kineticcore.feature.attribute.config;

import java.math.BigDecimal;

/** Text codec used for attribute bounds that do not fit a compact decimal editor. */
final class AttributeBoundaryCodec {
    /**
     * Longer plain decimals are difficult to read and may exceed an edit
     * box's practical input length. The text editor keeps the canonical
     * {@link Double#toString(double)} representation instead, which is both
     * compact and guaranteed to round-trip to the same double value.
     */
    private static final int MAX_COMPACT_PLAIN_LENGTH = 32;

    private AttributeBoundaryCodec() {
    }

    static boolean needsTextEditor(double value) {
        if (!Double.isFinite(value)) return true;
        // BigDecimal intentionally normalizes negative zero. Keep this rare
        // value on the lossless text path as well.
        if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0D)) return true;
        String plain = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        return plain.length() > MAX_COMPACT_PLAIN_LENGTH;
    }

    static boolean needsTextEditor(double defaultValue, double currentValue) {
        return needsTextEditor(defaultValue) || needsTextEditor(currentValue);
    }

    static String format(double value) {
        if (value == Double.POSITIVE_INFINITY) return "Infinity";
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity";
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("NaN is not a valid attribute boundary");
        }
        return Double.toString(value);
    }

    static double parse(String rawValue) {
        if (rawValue == null) throw new IllegalArgumentException("Attribute boundary is missing");
        String value = rawValue.trim();
        if (value.equalsIgnoreCase("inf")
                || value.equalsIgnoreCase("+inf")
                || value.equalsIgnoreCase("infinity")
                || value.equalsIgnoreCase("+infinity")) {
            return Double.POSITIVE_INFINITY;
        }
        if (value.equalsIgnoreCase("-inf") || value.equalsIgnoreCase("-infinity")) {
            return Double.NEGATIVE_INFINITY;
        }
        if (value.isEmpty()) throw new IllegalArgumentException("Attribute boundary is empty");

        final double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid attribute boundary: " + rawValue, exception);
        }
        if (!Double.isFinite(parsed)) {
            throw new IllegalArgumentException("Attribute boundary must be finite or an explicit Infinity token");
        }
        return parsed;
    }
}
