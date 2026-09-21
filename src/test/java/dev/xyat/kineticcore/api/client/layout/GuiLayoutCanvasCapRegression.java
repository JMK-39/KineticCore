package dev.xyat.kineticcore.api.client.layout;

/** Ensures no virtual design can bypass the 640x360 maximum via layout measurement. */
public final class GuiLayoutCanvasCapRegression {
    private static int checks;

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        var large = GuiLayout.measure(2560, 1440, 840f, 470f);
        check(large.designWidth() == 640f, "oversized design width must be capped to 640");
        check(large.designHeight() == 360f, "oversized design height must be capped to 360");
        check(large.fitScale() == 4f, "scale must use capped dimensions");
        var mixed = GuiLayout.measure(1280, 720, 320f, 999f);
        check(mixed.designWidth() == 320f, "a smaller width must be preserved");
        check(mixed.designHeight() == 360f, "height must be capped independently");
        var small = GuiLayout.measure(1280, 720, 200f, 100f);
        check(small.designWidth() == 200f && small.designHeight() == 100f,
                "legitimate small designs must not expand to 640x360");
        System.out.println("CANVAS_MAX_REGRESSION=" + checks + " PASS");
    }
}
