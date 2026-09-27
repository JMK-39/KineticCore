package dev.xyat.kineticcore.api.client.gui.input;

/**
 * 滚轮输入，坐标为页面坐标；deltaY 正值表示向上滚动。
 * Wheel input in page coordinates; a positive deltaY scrolls up.
 */
public record ScrollInput(double x, double y, double deltaX, double deltaY) {
    /** 点是否在矩形内 / Whether the pointer lies in the rectangle. */
    public boolean inside(int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
