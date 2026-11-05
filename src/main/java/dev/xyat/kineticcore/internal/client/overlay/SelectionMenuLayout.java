package dev.xyat.kineticcore.internal.client.overlay;

/** Pixel allocation for selection rows; kept independent of Minecraft rendering. */
final class SelectionMenuLayout {
    private SelectionMenuLayout() { }

    /**
     * Button menus always use real buttons, including multi-toggle menus. Only selection candidates presented
     * together with a search field use text rows. Addons inherit this rule from the core overlay API.
     */
    static boolean textRows(boolean selectable, boolean singleChoice, int togglesInMenu, boolean searchable) {
        if (!selectable) return false;
        return searchable;
    }

    static int gap(boolean selectionRow) {
        // Button menus keep 2 px between buttons too, so their rounded outlines never touch.
        return 2;
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
