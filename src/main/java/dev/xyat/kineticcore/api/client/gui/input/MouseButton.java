package dev.xyat.kineticcore.api.client.gui.input;

/** 鼠标按键 / Mouse button. */
public enum MouseButton {
    /** 左键（主键）/ Left (primary) button. */
    LEFT,
    /** 右键（次键）/ Right (secondary) button. */
    RIGHT,
    /** 中键 / Middle button. */
    MIDDLE,
    /** 其他侧键 / Any other button. */
    OTHER;

    /** 由平台原始按键编号转换 / Converts a raw platform button number. */
    public static MouseButton of(int rawButton) {
        return switch (rawButton) {
            case 0 -> LEFT;
            case 1 -> RIGHT;
            case 2 -> MIDDLE;
            default -> OTHER;
        };
    }
}
