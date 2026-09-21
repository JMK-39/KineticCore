package dev.xyat.kineticcore.api.client.layout;

import java.util.List;

/** Zero-weight panels must never steal remaining space from positive-weight panels. */
public final class WeightedLayoutZeroWeightRegression {
    public static void main(String[] args) {
        GuiLayout.Rect onePixel = new GuiLayout.Rect(10, 20, 1, 4);
        List<GuiLayout.Rect> columns = GuiLayout.weightedColumns(onePixel, 0, 1, 1, 1, 0);
        check(columns.size() == 4, "incorrect column count");
        check(columns.get(3).width() == 0, "zero-weight last column received layout width");
        check(columns.stream().mapToInt(GuiLayout.Rect::width).sum() == 1, "layout lost the available pixel");
        List<GuiLayout.Rect> rows = GuiLayout.weightedRows(new GuiLayout.Rect(0, 0, 4, 1), 0, 1, 1, 1, -3);
        check(rows.get(3).height() == 0, "negative-weight last row received layout height");
        check(rows.stream().mapToInt(GuiLayout.Rect::height).sum() == 1, "row layout lost available pixel");
        List<GuiLayout.Rect> ordinary = GuiLayout.weightedColumns(new GuiLayout.Rect(0, 0, 8, 5), 0, 1, 1);
        check(ordinary.get(0).width() == 4 && ordinary.get(1).width() == 4, "ordinary weighted sizing changed");
        System.out.println("PASS: 6 zero-weight layout checks");
    }
    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
