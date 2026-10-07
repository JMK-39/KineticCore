package dev.xyat.kineticcore.api.client.gui.widget.list;



/**
 * Density presets for scrollable item grids. Grids take their slot size, spacing and item scale from the preset, so
 * add-ons pick a preset instead of hard-coding pixel sizes.
 */
public enum ItemGridDensity {
    /** Native 16px icons in 22px slots, with 2px gaps and 2px viewport padding. */
    COMPACT(22, 2, 2, 1.0F, false),
    /** Default 24px slots with comfortable spacing. */
    STANDARD(24, 5, 4, 1.0F, false),
    /** 26px slots with extra spacing, for touch-friendly or sparse grids. */
    LARGE(26, 6, 6, 1.0F, false),
    /** Large 60px preview tiles with items drawn at 2.7x and stack decorations shown. */
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

    /** Returns the slot width and height in GUI pixels. */
    public int slotSize() {
        return slotSize;
    }

    /** Returns the space between neighbouring slots in GUI pixels. */
    public int gap() {
        return gap;
    }

    /** Returns the inner padding between the grid edge and the first slot in GUI pixels. */
    public int padding() {
        return padding;
    }

    /** Returns the scale used to draw items inside a slot; {@code 1.0} is the vanilla 16px item size. */
    public float renderScale() {
        return renderScale;
    }

    /** Returns whether stack counts and durability bars are drawn. */
    public boolean decorations() {
        return decorations;
    }

    /** Returns the distance from one slot's origin to the next: {@code slotSize() + gap()}. */
    public int cellPitch() {
        return slotSize + gap;
    }
}
