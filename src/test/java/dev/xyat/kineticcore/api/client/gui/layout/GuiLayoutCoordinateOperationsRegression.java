package dev.xyat.kineticcore.api.client.gui.layout;

import java.util.List;

/** Regression: an overflow in child positions must never wrap to the opposite screen side. */
public final class GuiLayoutCoordinateOperationsRegression {
    private static int checks;

    public static void main(String[] args) {
        KineticLayout.Rect nearRight = new KineticLayout.Rect(Integer.MAX_VALUE - 3, 10, 20, 20);
        KineticLayout.Rect nearBottom = new KineticLayout.Rect(10, Integer.MAX_VALUE - 3, 20, 20);
        check(nearRight.move(20, 0).x() == Integer.MAX_VALUE, "move positive x");
        check(new KineticLayout.Rect(Integer.MIN_VALUE + 3, 0, 20, 20).move(-20, 0).x() == Integer.MIN_VALUE, "move negative x");
        check(nearRight.inset(10, 0).x() == Integer.MAX_VALUE, "inset x");
        check(nearBottom.inset(0, 10).y() == Integer.MAX_VALUE, "inset y");
        check(KineticLayout.splitHorizontal(nearRight, 10, 3).second().x() == Integer.MAX_VALUE, "horizontal split");
        check(KineticLayout.splitVertical(nearBottom, 10, 3).second().y() == Integer.MAX_VALUE, "vertical split");
        check(safe(KineticLayout.columns(nearRight, 3, 1)), "equal columns");
        check(safe(KineticLayout.rows(nearBottom, 3, 1)), "equal rows");
        check(safe(KineticLayout.weightedColumns(nearRight, 1, 2, 3, 4)), "weighted columns");
        check(safe(KineticLayout.weightedRows(nearBottom, 1, 2, 3, 4)), "weighted rows");
        check(new KineticLayout.Rect(10, 20, 100, 50).move(5, 6).equals(new KineticLayout.Rect(15, 26, 100, 50)), "ordinary move");
        check(KineticLayout.splitHorizontal(new KineticLayout.Rect(10, 20, 100, 50), 40, 5).second().x() == 55, "ordinary split");
        System.out.println("PASS: " + checks + " coordinate operation regressions");
    }

    private static boolean safe(List<KineticLayout.Rect> children) {
        return children.stream().allMatch(r -> r.x() >= 0 && r.y() >= 0);
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
}
