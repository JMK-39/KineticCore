package dev.xyat.kineticcore.api.client.layout;

/** An add-on's far-edge rectangle must never wrap into the opposite side of the canvas. */
public final class GuiLayoutCoordinateOverflowRegression {
    public static void main(String[] args) {
        GuiLayout.Rect far = new GuiLayout.Rect(Integer.MAX_VALUE - 3, Integer.MAX_VALUE - 2, 50, 30);
        check(far.right() >= far.x(), "right edge wrapped negative");
        check(far.bottom() >= far.y(), "bottom edge wrapped negative");
        check(far.contains(Integer.MAX_VALUE - 1.0, Integer.MAX_VALUE - 1.0), "valid point rejected after overflow");
        check(!far.contains(-1, -1), "overflow accepted a negative-coordinate point");
        GuiLayout.Rect ordinary = new GuiLayout.Rect(5, 6, 10, 12);
        check(ordinary.right() == 15 && ordinary.bottom() == 18, "ordinary geometry changed");
        System.out.println("PASS: 5 rectangle-coordinate overflow cases");
    }
    private static void check(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }
}
