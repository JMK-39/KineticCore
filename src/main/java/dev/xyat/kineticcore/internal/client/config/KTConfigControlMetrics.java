package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.*;

import net.minecraft.client.gui.Font;

final class KTConfigControlMetrics {
    static final int CONTROL_RIGHT = 612;
    static final int CONTROL_GAP = 8;
    static final int RESET_WIDTH = 60;
    static final int RESET_X = CONTROL_RIGHT - RESET_WIDTH;
    static final int ACTION_WIDTH = 60;

    // Each control type has one width in every language and for every value, so the value column always lines up;
    // values and option names longer than the control scroll inside it.
    private static final int NUMERIC_WIDTH = 90;
    private static final int STRING_WIDTH = 132;
    private static final int CHOICE_WIDTH = 120;

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
            case INTEGER, LONG, DOUBLE -> NUMERIC_WIDTH;
            case STRING -> STRING_WIDTH;
            case CHOICE -> CHOICE_WIDTH;
            case STRING_LIST, ITEM_LIST, ITEM_RULE_LIST, ENTITY_LIST, INTEGER_LIST -> 120;
            case COLOR -> 104;
            default -> 132;
        };
    }
}
