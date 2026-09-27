package dev.xyat.kineticcore.api.client.gui.input;

/** 平台修饰键位掩码工具 / Helpers for the platform modifier bit mask. */
public final class KeyModifiers {
    /** Shift 位 / Shift bit. */
    public static final int SHIFT = 1;
    /** Ctrl 位 / Control bit. */
    public static final int CONTROL = 2;
    /** Alt 位 / Alt bit. */
    public static final int ALT = 4;
    /** Super/Win/Cmd 位 / Super bit. */
    public static final int SUPER = 8;

    private KeyModifiers() {
    }

    /** 是否按下 Shift / Whether Shift is held. */
    public static boolean shift(int modifiers) {
        return (modifiers & SHIFT) != 0;
    }

    /** 是否按下 Ctrl / Whether Control is held. */
    public static boolean control(int modifiers) {
        return (modifiers & CONTROL) != 0;
    }

    /** 是否按下 Alt / Whether Alt is held. */
    public static boolean alt(int modifiers) {
        return (modifiers & ALT) != 0;
    }
}
