package dev.xyat.kineticcore.api.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

public class NumericAutoCompleteBox extends AutoCompleteBox {
    public enum Type {
        INTEGER,
        DECIMAL
    }

    private final Type type;
    private final boolean allowNegative;
    private final Double minValue;
    private final Double maxValue;

    public NumericAutoCompleteBox(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Type type,
            boolean allowNegative,
            Double minValue,
            Double maxValue
    ) {
        super(
                font,
                x,
                y,
                width,
                height,
                message,
                dictionarySupplier
        );

        this.type = type;
        this.allowNegative = allowNegative;
        this.minValue = minValue;
        this.maxValue = maxValue;
        setFilter(this::isAllowedText);
    }

    public static NumericAutoCompleteBox integer(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue
    ) {
        return new NumericAutoCompleteBox(
                font,
                x,
                y,
                width,
                height,
                message,
                dictionarySupplier,
                Type.INTEGER,
                allowNegative,
                minValue == null
                        ? null
                        : minValue.doubleValue(),
                maxValue == null
                        ? null
                        : maxValue.doubleValue()
        );
    }

    public static NumericAutoCompleteBox decimal(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue
    ) {
        return new NumericAutoCompleteBox(
                font,
                x,
                y,
                width,
                height,
                message,
                dictionarySupplier,
                Type.DECIMAL,
                allowNegative,
                minValue,
                maxValue
        );
    }

    public Integer getIntValue() {
        String raw = getValue().trim();

        if (raw.isEmpty()
                || "-".equals(raw)) {
            return null;
        }

        try {
            int value = Integer.parseInt(raw);

            return isInRange(value)
                    ? value
                    : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public Double getDoubleValue() {
        String raw = getValue().trim();

        if (raw.isEmpty()
                || "-".equals(raw)
                || ".".equals(raw)
                || "-.".equals(raw)) {
            return null;
        }

        try {
            double value =
                    Double.parseDouble(raw);

            if (!Double.isFinite(value)
                    || !isInRange(value)) {
                return null;
            }

            return value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void setIntValue(int value) {
        setValue(Integer.toString(value));
    }

    public void setDoubleValue(double value) {
        setValue(format(value));
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }

        return BigDecimal.valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private boolean isAllowedText(String value) {
        if (value == null
                || value.isEmpty()) {
            return true;
        }

        if ("-".equals(value)) {
            return allowNegative;
        }

        int start =
                value.charAt(0) == '-'
                        ? 1
                        : 0;

        if (start == 1 && !allowNegative) {
            return false;
        }

        if (type == Type.INTEGER) {
            for (int i = start;
                 i < value.length();
                 i++) {
                if (!Character.isDigit(
                        value.charAt(i)
                )) {
                    return false;
                }
            }

            return true;
        }

        boolean dotSeen = false;

        for (int i = start;
             i < value.length();
             i++) {
            char c = value.charAt(i);

            if (c == '.') {
                if (dotSeen) {
                    return false;
                }

                dotSeen = true;
                continue;
            }

            if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    private boolean isInRange(double value) {
        if (minValue != null
                && value < minValue) {
            return false;
        }

        return maxValue == null
                || value <= maxValue;
    }
}
