package dev.xyat.kineticcore.internal.client.overlay;

/** Pixel allocation for selection rows; kept independent of Minecraft rendering. */
final class SelectionMenuLayout {
    private SelectionMenuLayout() { }

    static int gap(boolean selectionRow) {
        return selectionRow ? 2 : 0;
    }

    static Columns columns(int available, int labelWidth, int detailWidth) {
        int width = Math.max(0, available);
        if (detailWidth <= 0 || width <= 4) return new Columns(width, 0, 0);
        int gap = 2;
        int usable = width - gap;
        int detail = Math.min(Math.max(1, detailWidth), Math.max(1, usable / 2));
        if (labelWidth < usable - detail) detail = Math.min(detailWidth, usable - Math.max(1, labelWidth));
        return new Columns(usable - detail, gap, detail);
    }

    record Columns(int labelWidth, int gap, int detailWidth) { }
}
