package dev.xyat.kineticcore.api.client.gui.input;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;

/** 一次按键按下或松开 / One key press or release. */
public record KeyInput(int keyCode, int scanCode, int modifiers) {
    /** 是否为指定按键 / Whether this is the given key. */
    public boolean is(KineticKeyBindings.Key key) {
        return KineticKeyBindings.matchesKeyCode(key, keyCode);
    }

    /** 是否 Esc / Whether this is Escape. */
    public boolean isEscape() {
        return is(KineticKeyBindings.Key.ESCAPE);
    }

    /** 是否回车（含小键盘）/ Whether this is Enter (including keypad). */
    public boolean isEnter() {
        return is(KineticKeyBindings.Key.ENTER) || is(KineticKeyBindings.Key.KP_ENTER);
    }

    /** 是否按住 Shift / Whether Shift is held. */
    public boolean hasShift() {
        return KeyModifiers.shift(modifiers);
    }

    /** 是否按住 Ctrl / Whether Control is held. */
    public boolean hasControl() {
        return KeyModifiers.control(modifiers);
    }

    /** 是否按住 Alt / Whether Alt is held. */
    public boolean hasAlt() {
        return KeyModifiers.alt(modifiers);
    }
}
