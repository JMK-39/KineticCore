package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.*;

import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import net.minecraft.client.gui.Font;

final class KTConfigControlMetrics {
    static final int CONTROL_RIGHT = 612;
    static final int CONTROL_GAP = 8;
    static final int RESET_WIDTH = 60;
    static final int RESET_X = CONTROL_RIGHT - RESET_WIDTH;
    static final int ACTION_WIDTH = 60;

    private static final int NUMERIC_MIN_WIDTH = 60;
    private static final int NUMERIC_MAX_WIDTH = 120;
    private static final int STRING_MIN_WIDTH = 92;
    private static final int STRING_MAX_WIDTH = 176;
    private static final int CHOICE_MIN_WIDTH = 76;
    private static final int CHOICE_MAX_WIDTH = 160;
    private static final int TEXT_PADDING = 14;

    private KTConfigControlMetrics() {
    }

    static int editorX(int width) {
        return RESET_X - CONTROL_GAP - width;
    }

    static int actionX() {
        return CONTROL_RIGHT - ACTION_WIDTH;
    }

    static int editorWidth(Font font, KTConfigEntry<?> entry, Object currentValue) {
        return switch (entry.type()) {
            case BOOLEAN, LONG_TEXT -> ACTION_WIDTH;
            case INTEGER, LONG, DOUBLE -> numericWidth(font, entry, currentValue);
            case STRING -> stringWidth(font, entry, currentValue);
            case CHOICE -> choiceWidth(font, entry);
            case STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, ENTITY_LIST, INTEGER_LIST -> 120;
            case COLOR -> 104;
            default -> 132;
        };
    }

    private static int numericWidth(Font font, KTConfigEntry<?> entry, Object currentValue) {
        int contentWidth = valueWidth(font, entry.type(), currentValue);
        contentWidth = Math.max(contentWidth, valueWidth(font, entry.type(), entry.defaultValue()));

        Number minimum = entry.minimum();
        Number maximum = entry.maximum();
        if (minimum != null && !isUnboundedMinimum(entry.type(), minimum)) {
            contentWidth = Math.max(contentWidth, valueWidth(font, entry.type(), minimum));
        }
        if (maximum != null && !isUnboundedMaximum(entry.type(), maximum)) {
            contentWidth = Math.max(contentWidth, valueWidth(font, entry.type(), maximum));
        }

        return clamp(contentWidth + TEXT_PADDING, NUMERIC_MIN_WIDTH, NUMERIC_MAX_WIDTH);
    }

    private static int stringWidth(Font font, KTConfigEntry<?> entry, Object currentValue) {
        int contentWidth = font.width(stringValue(currentValue));
        contentWidth = Math.max(contentWidth, font.width(stringValue(entry.defaultValue())));
        return clamp(contentWidth + TEXT_PADDING, STRING_MIN_WIDTH, STRING_MAX_WIDTH);
    }


    private static int choiceWidth(Font font, KTConfigEntry<?> entry) {
        int contentWidth = entry.choiceOptions().stream()
                .mapToInt(option -> font.width(option.label()))
                .max()
                .orElse(0);
        return clamp(contentWidth + TEXT_PADDING + 10, CHOICE_MIN_WIDTH, CHOICE_MAX_WIDTH);
    }

    private static int valueWidth(Font font, KTConfigEntry.Type type, Object value) {
        if (value == null) return 0;
        if (type == KTConfigEntry.Type.DOUBLE && value instanceof Number number) {
            return font.width(NumericEditBox.format(number.doubleValue()));
        }
        return font.width(String.valueOf(value));
    }

    private static boolean isUnboundedMinimum(KTConfigEntry.Type type, Number value) {
        return switch (type) {
            case INTEGER -> value.intValue() == Integer.MIN_VALUE;
            case LONG -> value.longValue() == Long.MIN_VALUE;
            case DOUBLE -> value.doubleValue() <= -Double.MAX_VALUE;
            default -> false;
        };
    }

    private static boolean isUnboundedMaximum(KTConfigEntry.Type type, Number value) {
        return switch (type) {
            case INTEGER -> value.intValue() == Integer.MAX_VALUE;
            case LONG -> value.longValue() == Long.MAX_VALUE;
            case DOUBLE -> value.doubleValue() >= Double.MAX_VALUE;
            default -> false;
        };
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
