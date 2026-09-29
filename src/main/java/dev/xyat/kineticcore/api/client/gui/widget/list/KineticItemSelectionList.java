package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Smooth vertical single-selection list with a standard item slot in every row. */
public interface KineticItemSelectionList extends KineticControl {
    /**
     * Replaces every row. Scrolling, clipping and selection rendering stay managed by the list; the selection is
     * kept when its index still exists.
     */
    void setItems(List<? extends ItemSelectionItem> items);

    /** Returns the rows currently displayed, as an immutable list. */
    List<ItemSelectionItem> items();

    /** Returns the selected row index, or {@code -1} when no row is selected. */
    int selectedIndex();

    /**
     * Selects a row without calling the selection responder. Out-of-range indexes are clamped; {@code -1} clears
     * the selection.
     */
    void setSelectedIndex(int index);

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

    /**
     * Returns the stack in the item slot under the pointer, or {@link ItemStack#EMPTY} when the pointer is not over
     * a slot.
     */
    ItemStack stackAt(double mouseX, double mouseY);

    /** Returns the stack of the slot hovered during the last render, or {@link ItemStack#EMPTY}. */
    ItemStack hoveredStack();

    /**
     * Returns the tooltip of the hovered row, action or scrollbar, or {@code null} when nothing with a tooltip is
     * hovered.
     */
    Component hoveredTooltip();
}
