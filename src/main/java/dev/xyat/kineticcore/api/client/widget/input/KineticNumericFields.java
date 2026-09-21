package dev.xyat.kineticcore.api.client.widget.input;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import java.util.function.Predicate;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.input.NumericInputRules;

/** Numeric input controls used by Kinetic screen and detached-widget factories. */
public final class KineticNumericFields {
    private KineticNumericFields() {}

    /** Formats a decimal value with the same stable representation used by Kinetic decimal input fields. */
    public static String formatDecimal(double value) {
        return NumericInputRules.formatDouble(value);
    }

    /** Numeric syntax supported by Kinetic numeric fields. Range and sign policy remain caller-defined. */
    public enum Type {
        INTEGER,
        LONG,
        DECIMAL
    }

    /** Standard Kinetic numeric text field with explicit numeric validation rules. */
    public static class NumericEditBox extends KineticEditBox {

        private final Type type;
        private final boolean allowNegative;
        private final Number minValue;
        private final Number maxValue;
        private final Predicate<Number> validator;

        /** Creates a new {@code NumericEditBox}. */
        public NumericEditBox(
                FactoryAccess access,
                Font font,
                int x,
                int y,
                int width,
                int height,
                Component message,
                Type type,
                boolean allowNegative,
                Number minValue,
                Number maxValue,
                Predicate<Number> validator
        ) {
            super(access, font, x, y, width, height, message);
            this.type = type;
            this.allowNegative = allowNegative;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.validator = validator == null ? ignored -> true : validator;
            setFilter(this::isAllowedText);
        }

        /** Returns the validated integer value, or {@code null} while the text is incomplete or invalid. */
        public Integer getIntValue() {
            return NumericInputRules.integerValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns the validated long value, or {@code null} while the text is incomplete or invalid. */
        public Long getLongValue() {
            return NumericInputRules.longValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns the validated decimal value, or {@code null} while the text is incomplete or invalid. */
        public Double getDoubleValue() {
            return NumericInputRules.doubleValue(getValue(), minValue, maxValue, validator);
        }

        /** Returns whether the current raw input satisfies this field's numeric validation rules. */
        public boolean isValueValid() {
            return switch (type) {
                case INTEGER -> getIntValue() != null;
                case LONG -> getLongValue() != null;
                case DECIMAL -> getDoubleValue() != null;
            };
        }

        /** Sets int value. */
        public void setIntValue(int value) {
            setValue(Integer.toString(value));
        }

        /** Sets long value. */
        public void setLongValue(long value) {
            setValue(Long.toString(value));
        }

        /** Sets a decimal value using Kinetic's stable non-scientific formatting when possible. */
        public void setDoubleValue(double value) {
            setValue(formatDecimal(value));
        }

        private boolean isAllowedText(String value) {
            return NumericInputRules.isAllowedText(value, type, allowNegative);
        }

        @Override
        protected boolean hasBorderError() {
            return super.hasBorderError() || !isValueValid();
        }
    }

}
