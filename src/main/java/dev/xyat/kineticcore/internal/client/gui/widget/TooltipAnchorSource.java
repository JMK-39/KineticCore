package dev.xyat.kineticcore.internal.client.gui.widget;

/**
 * A control made of rows or cells (lists, grids) tells the screen which part its tooltip is about, so the tooltip is
 * placed beside that row or cell instead of over it.
 */
public interface TooltipAnchorSource {
    /** The hovered row or cell as {x, y, width, height} in the control's coordinates, or null for none. */
    int[] tooltipAnchor(double mouseX, double mouseY);
}
