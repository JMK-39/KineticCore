package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Smooth vertical multi-toggle list using standard Kinetic state buttons, clipping, and scrollbar behavior.
 */
public interface KineticToggleList extends KineticControl {
    /**
     * Replaces every row. Scrolling, clipping and selection rendering stay managed by the list; the selection is
     * kept when its index still exists.
     */
    void setItems(List<? extends ToggleItem> items);

    /** Returns the rows currently displayed, as an immutable list. */
    List<ToggleItem> items();

    /**
     * Returns the state of a row's toggle.
     *
     * @throws IndexOutOfBoundsException if {@code index} is not a row index
     */
    boolean value(int index);

    /**
     * Sets one row's toggle without calling the change responder, for syncing with external data.
     *
     * @throws IndexOutOfBoundsException if {@code index} is not a row index
     */
    void setValue(int index, boolean value);

    /**
     * Sets every row's toggle at once without calling the change responder; {@code null} elements count as
     * {@code false}.
     *
     * @param values one value per row, in row order
     * @throws IllegalArgumentException if {@code values} is {@code null} or its size differs from the row count
     */
    void setValues(List<Boolean> values);

    /** Moves and resizes the whole list viewport in canvas coordinates. */
    void setBounds(int x, int y, int width, int height);

    /** Returns the current scroll position as the index of the first visible row. */
    int scrollOffset();

    /** Jumps to a scroll position without animation, clamped to {@code 0..maxScrollOffset()}. */
    void setScrollOffset(int offset);

    /** Returns the largest scroll position for the current rows and height; {@code 0} when everything fits. */
    int maxScrollOffset();

    /** Returns the index of the row under the pointer, or {@code -1} when none is. */
    int itemAt(double mouseX, double mouseY);

    /**
     * Returns the tooltip of the hovered row, action or scrollbar, or {@code null} when nothing with a tooltip is
     * hovered.
     */
    Component hoveredTooltip();
}
