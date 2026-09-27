package dev.xyat.kineticcore.api.client.gui.input;

/**
 * 一次鼠标按下或松开，坐标为页面坐标。
 * One mouse press or release in page coordinates.
 */
public record MouseInput(double x, double y, MouseButton button, int rawButton, int modifiers) {
    /** 是否左键 / Whether this is the left button. */
    public boolean isLeft() {
        return button == MouseButton.LEFT;
    }

    /** 是否右键 / Whether this is the right button. */
    public boolean isRight() {
        return button == MouseButton.RIGHT;
    }

    /** 是否中键 / Whether this is the middle button. */
    public boolean isMiddle() {
        return button == MouseButton.MIDDLE;
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

    /** 点是否在矩形内 / Whether the pointer lies in the rectangle. */
    public boolean inside(int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
