package dev.xyat.kineticcore.api.client.layout;

import java.util.List;

/** All layout primitives must stay inside their source rectangle for valid small canvases. */
public final class GuiLayoutBoundsRegression {
    private static int cases;
    private static void check(boolean result, String message) {
        cases++;
        if (!result) throw new AssertionError(message);
    }
    private static void within(GuiLayout.Rect outer, List<GuiLayout.Rect> rects) {
        for (GuiLayout.Rect rect : rects) {
            check(rect.x() >= outer.x() && rect.y() >= outer.y()
                    && rect.right() <= outer.right() && rect.bottom() <= outer.bottom(),
                    "layout escaped its containing rectangle: " + rect);
        }
    }
    public static void main(String[] args) {
        var area = new GuiLayout.Rect(10, 10, 100, 40);
        within(area, GuiLayout.columns(area, 3, 10));
        within(area, GuiLayout.weightedColumns(area, 10, 2, 1, 1));
        within(area, GuiLayout.columns(area, 3, Integer.MAX_VALUE));
        within(area, GuiLayout.weightedColumns(area, Integer.MAX_VALUE, 2, 1, 1));
        within(area, GuiLayout.weightedColumns(area, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
        within(area, GuiLayout.weightedRows(area, Integer.MAX_VALUE, 1, 1));
        within(area, List.of(GuiLayout.splitHorizontal(area, 40, Integer.MAX_VALUE).first(),
                GuiLayout.splitHorizontal(area, 40, Integer.MAX_VALUE).second()));
        within(area, List.of(GuiLayout.splitVertical(area, 15, Integer.MAX_VALUE).first(),
                GuiLayout.splitVertical(area, 15, Integer.MAX_VALUE).second()));
        within(area, List.of(area.inset(Integer.MAX_VALUE, Integer.MAX_VALUE)));
        var huge = GuiLayout.weightedColumns(new GuiLayout.Rect(0, 0, 100, 20), 0,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        check(huge.get(0).width() == 50 && huge.get(1).width() == 50,
                "weight sum overflow must not turn an even split into an accidental fallback");
        var invalid = GuiLayout.measure(800, 600, Float.NaN, Float.POSITIVE_INFINITY);
        check(Float.isFinite(invalid.fitScale()) && invalid.fitScale() > 0,
                "invalid external layout dimensions cannot poison screen scaling");
        System.out.println("PASS: " + cases + " canvas layout boundary checks");
    }
}
