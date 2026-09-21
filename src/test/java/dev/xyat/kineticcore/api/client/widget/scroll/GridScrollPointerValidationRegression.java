package dev.xyat.kineticcore.api.client.widget.scroll;

/** Pointer coordinates from GUI integrations must not start or corrupt a drag when non-finite. */
public final class GridScrollPointerValidationRegression {
    private static int checks;
    public static void main(String[] args) {
        KineticScroll.GridScrollController grid = new KineticScroll.GridScrollController();
        grid.update(40, 4);
        grid.setOffset(10);
        check(!grid.beginDrag(Double.NaN, 20, 0, 0, 6, 120, 12, 0), "reject NaN start x");
        check(!grid.beginDrag(2, Double.NaN, 0, 0, 6, 120, 12, 0), "reject NaN start y");
        check(!grid.beginDrag(Double.POSITIVE_INFINITY, 20, 0, 0, 6, 120, 12, 0), "reject infinite start x");
        check(!grid.release(0), "invalid vertical begin must not leave a drag");
        check(grid.beginDrag(2, 20, 0, 0, 6, 120, 12, 0), "start valid vertical drag");
        int before = grid.offset();
        check(!grid.drag(Double.NaN, 0, 120, 12), "reject invalid vertical move");
        check(grid.offset() == before, "invalid vertical move preserves offset");
        check(grid.release(0), "valid vertical drag remains releasable");
        check(!grid.beginHorizontalDrag(Double.NaN, 2, 0, 0, 120, 6, 12, 0), "reject NaN horizontal start x");
        check(!grid.beginHorizontalDrag(20, Double.NEGATIVE_INFINITY, 0, 0, 120, 6, 12, 0), "reject infinite horizontal start y");
        check(!grid.release(0), "invalid horizontal begin must not leave a drag");
        check(grid.beginHorizontalDrag(20, 2, 0, 0, 120, 6, 12, 0), "start valid horizontal drag");
        check(!grid.dragHorizontal(Double.NaN, 0, 120, 12), "reject invalid horizontal move");
        check(grid.release(0), "valid horizontal drag remains releasable");
        System.out.println("PASS: " + checks + " grid pointer validation regressions");
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
        checks++;
    }
}
