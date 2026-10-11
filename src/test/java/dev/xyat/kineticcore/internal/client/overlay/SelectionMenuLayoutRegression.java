package dev.xyat.kineticcore.internal.client.overlay;

/** Covers long enum IDs, translated labels and narrow popups without a Minecraft bootstrap. */
public final class SelectionMenuLayoutRegression {
    public static void main(String[] args) {
        if (SelectionMenuLayout.gap(true) != 2) throw new AssertionError("Selection rows must have a 2-pixel gap");
        if (SelectionMenuLayout.gap(false) != 0) throw new AssertionError("Action menu spacing must stay unchanged");
        check(200, 160, 72);
        check(90, 160, 72);
        check(18, 160, 72);
        check(1, 160, 72);
        check(0, 160, 72);
        SelectionMenuLayout.Columns english = SelectionMenuLayout.columns(90, 160, 0);
        if (english.labelWidth() != 90 || english.detailWidth() != 0 || english.gap() != 0)
            throw new AssertionError("English rows must give all available space to the ID");
        SelectionMenuLayout.Columns fitted = SelectionMenuLayout.columns(240, 100, 72);
        if (fitted.labelWidth() < 100 || fitted.detailWidth() != 72)
            throw new AssertionError("Labels that fit must remain fully visible");
        System.out.println("Selection menu layout regressions passed.");
    }

    private static void check(int available, int label, int detail) {
        SelectionMenuLayout.Columns columns = SelectionMenuLayout.columns(available, label, detail);
        if (columns.labelWidth() < 0 || columns.detailWidth() < 0 || columns.gap() < 0
                || columns.labelWidth() + columns.detailWidth() + columns.gap() > available)
            throw new AssertionError("ID and translation overlap at width " + available + ": " + columns);
        if (available > 4 && (columns.labelWidth() == 0 || columns.detailWidth() == 0))
            throw new AssertionError("Both columns need readable space at width " + available);
    }
}
