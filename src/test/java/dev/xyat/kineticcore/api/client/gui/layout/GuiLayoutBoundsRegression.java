package dev.xyat.kineticcore.api.client.gui.layout;

import java.util.List;

/** All layout primitives must stay inside their source rectangle for valid small canvases. */
public final class GuiLayoutBoundsRegression {
    private static int cases;
    private static void check(boolean result, String message) {
        cases++;
        if (!result) throw new AssertionError(message);
    }
    private static void within(KineticLayout.Rect outer, List<KineticLayout.Rect> rects) {
        for (KineticLayout.Rect rect : rects) {
            check(rect.x() >= outer.x() && rect.y() >= outer.y()
                    && rect.right() <= outer.right() && rect.bottom() <= outer.bottom(),
                    "layout escaped its containing rectangle: " + rect);
        }
    }
    public static void main(String[] args) {
        var area = new KineticLayout.Rect(10, 10, 100, 40);
        within(area, KineticLayout.columns(area, 3, 10));
        within(area, KineticLayout.weightedColumns(area, 10, 2, 1, 1));
        within(area, KineticLayout.columns(area, 3, Integer.MAX_VALUE));
        within(area, KineticLayout.weightedColumns(area, Integer.MAX_VALUE, 2, 1, 1));
        within(area, KineticLayout.weightedColumns(area, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));
        within(area, KineticLayout.weightedRows(area, Integer.MAX_VALUE, 1, 1));
        within(area, List.of(KineticLayout.splitHorizontal(area, 40, Integer.MAX_VALUE).first(),
                KineticLayout.splitHorizontal(area, 40, Integer.MAX_VALUE).second()));
        within(area, List.of(KineticLayout.splitVertical(area, 15, Integer.MAX_VALUE).first(),
                KineticLayout.splitVertical(area, 15, Integer.MAX_VALUE).second()));
        within(area, List.of(area.inset(Integer.MAX_VALUE, Integer.MAX_VALUE)));
        var huge = KineticLayout.weightedColumns(new KineticLayout.Rect(0, 0, 100, 20), 0,
                Integer.MAX_VALUE, Integer.MAX_VALUE);
        check(huge.get(0).width() == 50 && huge.get(1).width() == 50,
                "weight sum overflow must not turn an even split into an accidental fallback");
        var invalid = KineticLayout.measure(800, 600, Float.NaN, Float.POSITIVE_INFINITY);
        check(Float.isFinite(invalid.fitScale()) && invalid.fitScale() > 0,
                "invalid external layout dimensions cannot poison screen scaling");
        System.out.println("PASS: " + cases + " canvas layout boundary checks");
    }
}
