package dev.xyat.kineticcore.api.client.tooltip;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;

/**
 * Client-side drawing of one custom tooltip component, for example a row of item icons below the tooltip text.
 * Register a factory for it with {@link KineticItemTooltips#registerComponentFactory}. All methods run on the render
 * thread.
 */
public interface KineticTooltipComponent {
    /** Returns the height in pixels this component takes in the tooltip. */
    int height();

    /** Returns the width in pixels this component needs; measure text with {@code KineticText.width}. */
    int width();

    /**
     * Draws the component with its top-left corner at the given position.
     *
     * @param graphics drawing context of the tooltip
     * @param x left edge
     * @param y top edge
     */
    void render(KineticGraphics graphics, int x, int y);
}
