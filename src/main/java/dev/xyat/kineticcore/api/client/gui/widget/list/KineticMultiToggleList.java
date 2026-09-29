package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Smooth vertical single-selection list with any number of real toggles per row. */
public interface KineticMultiToggleList extends KineticControl {
    /**
     * Replaces every row. Scrolling, clipping and selection rendering stay managed by the list; the selection is
     * kept when its index still exists.
     */
    void setItems(List<? extends MultiToggleItem> items);

    /** Returns the rows currently displayed, as an immutable list. */
    List<MultiToggleItem> items();

    /** Returns the selected row index, or {@code -1} when no row is selected. */
    int selectedIndex();

    /**
     * Selects a row without calling the selection responder. Out-of-range indexes are clamped; {@code -1} clears
     * the selection.
     */
    void setSelectedIndex(int index);

    /**
     * Returns the state of one toggle in a row.
     *
     * @throws IndexOutOfBoundsException if either index is out of range
     */
    boolean toggleValue(int rowIndex, int toggleIndex);

    /**
     * Sets one toggle without calling the toggle responder, for syncing with external data.
     *
     * @throws IndexOutOfBoundsException if either index is out of range
     */
    void setToggleValue(int rowIndex, int toggleIndex, boolean value);

    /** Moves and resizes the whole list viewport in canvas coordinates. */
    void setBounds(int x, int y, int width, int height);

    /** Returns the current scroll position as the index of the first visible row. */
    int scrollOffset();

    /** Jumps to a scroll position without animation, clamped to {@code 0..maxScrollOffset()}. */
    void setScrollOffset(int offset);

    /** Returns the largest scroll position for the current rows and height; {@code 0} when everything fits. */
    int maxScrollOffset();

    /** Scrolls the minimum amount needed to show the selected row; does nothing without a selection. */
    void ensureSelectedVisible();

    /** Returns the index of the row under the pointer, or {@code -1} when none is. */
    int itemAt(double mouseX, double mouseY);

    /** Returns which toggle is under the pointer, or {@code null} when none is. */
    ToggleHit toggleAt(double mouseX, double mouseY);

    /**
     * Returns the tooltip of the hovered row, action or scrollbar, or {@code null} when nothing with a tooltip is
     * hovered.
     */
    Component hoveredTooltip();
}
