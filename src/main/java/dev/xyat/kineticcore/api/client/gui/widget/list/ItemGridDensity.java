package dev.xyat.kineticcore.api.client.gui.widget.list;



/** API-defined density presets for scrollable item grids. */
public enum ItemGridDensity {
    COMPACT(18, 1, 0, 1.0F, false),
    STANDARD(24, 5, 4, 1.0F, false),
    LARGE(26, 6, 6, 1.0F, false),
    PREVIEW(60, 2, 0, 2.7F, true);

    private final int slotSize;
    private final int gap;
    private final int padding;
    private final float renderScale;
    private final boolean decorations;

    ItemGridDensity(int slotSize, int gap, int padding, float renderScale, boolean decorations) {
        this.slotSize = slotSize;
        this.gap = gap;
        this.padding = padding;
        this.renderScale = renderScale;
        this.decorations = decorations;
    }

    /**
     * Returns the slot size.
     */
    public int slotSize() {
        return slotSize;
    }

    /**
     * Performs the gap API operation.
     */
    public int gap() {
        return gap;
    }

    /**
     * Performs the padding API operation.
     */
    public int padding() {
        return padding;
    }

    /**
     * Renders scale.
     */
    public float renderScale() {
        return renderScale;
    }

    /**
     * Performs the decorations API operation.
     */
    public boolean decorations() {
        return decorations;
    }

    /**
     * Performs the cell pitch API operation.
     */
    public int cellPitch() {
        return slotSize + gap;
    }
}
