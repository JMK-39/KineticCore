package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Smooth scrollable item-slot grid with API-defined density and caller-owned selection semantics. */
public interface KineticItemGrid extends KineticControl {
    /** Replaces every slot. Scrolling and clipping stay managed by the grid. */
    void setItems(List<? extends ItemGridItem> items);

    /** Returns the slots currently displayed, as an immutable list. */
    List<ItemGridItem> items();

    /** Moves and resizes the whole list viewport in canvas coordinates. */
    void setBounds(int x, int y, int width, int height);

    /** Returns the current scroll position as the index of the first visible row of slots. */
    int scrollOffset();

    /** Jumps to a scroll position without animation, clamped to {@code 0..maxScrollOffset()}. */
    void setScrollOffset(int offset);

    /** Returns the largest scroll position for the current rows and height; {@code 0} when everything fits. */
    int maxScrollOffset();

    /** Returns how many slots fit in one row at the current width and density; at least 1. */
    int columns();

    /** Returns how many rows of slots fit in the current height; at least 1. */
    int visibleRows();

    /** Returns the index of the slot under the pointer, or {@code -1} when none is. */
    int itemAt(double mouseX, double mouseY);

    /** Returns the stack in the slot under the pointer, or {@link ItemStack#EMPTY}. */
    ItemStack stackAt(double mouseX, double mouseY);

    /** Returns the stack of the slot hovered during the last render, or {@link ItemStack#EMPTY}. */
    ItemStack hoveredStack();

    /**
     * Returns the custom tooltip of the hovered slot or the scrollbar hint, or {@code null}; item tooltips come
     * from {@link #hoveredStack()}.
     */
    Component hoveredTooltip();
}
