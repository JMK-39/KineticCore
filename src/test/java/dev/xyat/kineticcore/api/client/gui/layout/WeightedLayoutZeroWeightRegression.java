package dev.xyat.kineticcore.api.client.gui.layout;

import java.util.List;

/** Zero-weight panels must never steal remaining space from positive-weight panels. */
public final class WeightedLayoutZeroWeightRegression {
    public static void main(String[] args) {
        KineticLayout.Rect onePixel = new KineticLayout.Rect(10, 20, 1, 4);
        List<KineticLayout.Rect> columns = KineticLayout.weightedColumns(onePixel, 0, 1, 1, 1, 0);
        check(columns.size() == 4, "incorrect column count");
        check(columns.get(3).width() == 0, "zero-weight last column received layout width");
        check(columns.stream().mapToInt(KineticLayout.Rect::width).sum() == 1, "layout lost the available pixel");
        List<KineticLayout.Rect> rows = KineticLayout.weightedRows(new KineticLayout.Rect(0, 0, 4, 1), 0, 1, 1, 1, -3);
        check(rows.get(3).height() == 0, "negative-weight last row received layout height");
        check(rows.stream().mapToInt(KineticLayout.Rect::height).sum() == 1, "row layout lost available pixel");
        List<KineticLayout.Rect> ordinary = KineticLayout.weightedColumns(new KineticLayout.Rect(0, 0, 8, 5), 0, 1, 1);
        check(ordinary.get(0).width() == 4 && ordinary.get(1).width() == 4, "ordinary weighted sizing changed");
        System.out.println("PASS: 6 zero-weight layout checks");
    }
    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }
}
