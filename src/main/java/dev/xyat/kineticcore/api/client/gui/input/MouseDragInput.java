package dev.xyat.kineticcore.api.client.gui.input;

/** 按住按键拖动，坐标与位移为页面坐标 / A drag with a held button; position and delta are in page coordinates. */
public record MouseDragInput(double x, double y, MouseButton button, int rawButton, double deltaX, double deltaY) {
    /** 是否左键拖动 / Whether the left button is dragging. */
    public boolean isLeft() {
        return button == MouseButton.LEFT;
    }
}
