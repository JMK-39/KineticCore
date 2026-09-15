package dev.xyat.kineticcore.api.client.widget.input;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import java.math.BigDecimal;
import java.util.function.Predicate;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import static dev.xyat.kineticcore.api.client.widget.KineticWidgets.attachTooltip;

/** 控件实现分组；附属统一从 KineticWidgets 工厂进入。 */
public final class KineticNumericFields {
    private KineticNumericFields() {}

    public static NumericEditBox createIntegerField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return createIntegerField(font, x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericEditBox createIntegerField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = integer(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static NumericEditBox createLongField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return createLongField(font, x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericEditBox createLongField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = longInteger(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static NumericEditBox createDecimalField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return createDecimalField(font, x, y, width, message, allowNegative, minValue, maxValue, null, tooltip);
    }

    public static NumericEditBox createDecimalField(
            Font font,
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        NumericEditBox box = decimal(
                font, x, y, width, KineticScreen.STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue, validator
        );
        attachTooltip(box, tooltip);
        return box;
    }

    public static class NumericEditBox extends KineticEditBox {
        public enum Type {
            INTEGER,
            LONG,
            DECIMAL
        }


        private final NumericEditBox.Type type;
        private final boolean allowNegative;
        private final Number minValue;
        private final Number maxValue;
        private final Predicate<Number> validator;

        public NumericEditBox(
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                NumericEditBox.Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue
        ) {
            this(font, x, y, width, height, message, type, allowNegative, minValue, maxValue, null);
        }

        public NumericEditBox(
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                NumericEditBox.Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue,
                Predicate<Number> validator
        ) {
            super(font, x, y, width, height, message);
            this.type = type;
            this.allowNegative = allowNegative;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.validator = validator == null ? ignored -> true : validator;
            setFilter(this::isAllowedText);
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Integer getIntValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw)) return null;

            try {
                int value = Integer.parseInt(raw);
                return isInLongRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Long getLongValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw)) return null;

            try {
                long value = Long.parseLong(raw);
                return isInLongRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        /** 返回通过范围与业务校验的数值；空值、编辑中间态、格式错误或校验失败返回 null。 */
        public Double getDoubleValue() {
            String raw = getValue().trim();
            if (raw.isEmpty() || "-".equals(raw) || ".".equals(raw) || "-.".equals(raw)) return null;

            try {
                double value = Double.parseDouble(raw);
                return Double.isFinite(value) && isInDoubleRange(value) && validator.test(value) ? value : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        public boolean isValueValid() {
            return switch (type) {
                case INTEGER -> getIntValue() != null;
                case LONG -> getLongValue() != null;
                case DECIMAL -> getDoubleValue() != null;
            };
        }

        public void setIntValue(int value) {
            setValue(Integer.toString(value));
        }

        public void setLongValue(long value) {
            setValue(Long.toString(value));
        }

        public void setDoubleValue(double value) {
            setValue(format(value));
        }

        public static String format(double value) {
            if (!Double.isFinite(value)) return Double.toString(value);
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }

        private boolean isAllowedText(String value) {
            if (value == null || value.isEmpty()) return true;
            if ("-".equals(value)) return allowNegative;

            int start = value.charAt(0) == '-' ? 1 : 0;
            if (start == 1 && !allowNegative) return false;

            if (type != NumericEditBox.Type.DECIMAL) {
                for (int i = start; i < value.length(); i++) {
                    if (!Character.isDigit(value.charAt(i))) return false;
                }
                return true;
            }

            boolean dotSeen = false;
            for (int i = start; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '.') {
                    if (dotSeen) return false;
                    dotSeen = true;
                } else if (!Character.isDigit(c)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInLongRange(long value) {
            if (minValue != null && value < minValue.longValue()) return false;
            return maxValue == null || value <= maxValue.longValue();
        }

        private boolean isInDoubleRange(double value) {
            if (minValue != null && value < minValue.doubleValue()) return false;
            return maxValue == null || value <= maxValue.doubleValue();
        }

        @Override
        protected boolean hasBorderError() {
            return super.hasBorderError() || !isValueValid();
        }
    }

    private static NumericEditBox integer(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue
    ) {
        return integer(font, x, y, width, height, message, allowNegative, minValue, maxValue, null);
    }

    private static NumericEditBox integer(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator
    ) {
        return new NumericEditBox(
                font, x, y, width, height, message,
                NumericEditBox.Type.INTEGER, allowNegative, minValue, maxValue, validator
        );
    }

    private static NumericEditBox longInteger(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue
    ) {
        return longInteger(font, x, y, width, height, message, allowNegative, minValue, maxValue, null);
    }

    private static NumericEditBox longInteger(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator
    ) {
        return new NumericEditBox(
                font, x, y, width, height, message,
                NumericEditBox.Type.LONG, allowNegative, minValue, maxValue, validator
        );
    }

    private static NumericEditBox decimal(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue
    ) {
        return decimal(font, x, y, width, height, message, allowNegative, minValue, maxValue, null);
    }

    private static NumericEditBox decimal(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator
    ) {
        return new NumericEditBox(
                font, x, y, width, height, message,
                NumericEditBox.Type.DECIMAL, allowNegative, minValue, maxValue, validator
        );
    }
}
