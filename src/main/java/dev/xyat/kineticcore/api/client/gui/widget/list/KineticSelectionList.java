package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Smooth vertical single-selection list using standard Kinetic state buttons, clipping, and scrollbar behavior.
 */
public interface KineticSelectionList extends KineticControl {

    /** Replaces the complete row model while preserving API-managed scrolling and selection rendering. */
    void setItems(List<? extends SelectionItem> items);

    /** Returns the immutable row model currently displayed by this list. */
    List<SelectionItem> items();

    /**
     * Draws the rows as plain text rows (striped background, no button frames), for lists of identifiers such as
     * biomes, damage types or attributes. Rows are 14 px high with 2 px between rows.
     */
    void setTextRows(boolean textRows);

    /** Returns whether the rows are drawn as plain text rows. */
    boolean textRows();

    /** Returns the selected row index, or {@code -1} when no row is selected. */
    int selectedIndex();

    /** Synchronizes the selected row without invoking the selection responder. */
    void setSelectedIndex(int index);

    /** Repositions and resizes the complete list viewport. */
    void setBounds(int x, int y, int width, int height);

    /** Returns the nearest current logical row offset. */
    int scrollOffset();

    /** Immediately sets the logical row offset, clamped to the current content range. */
    void setScrollOffset(int offset);

    /** Returns the maximum logical row offset currently available. */
    int maxScrollOffset();

    /** Ensures the selected row is visible inside the current viewport. */
    void ensureSelectedVisible();

    /** Returns the row index currently under the pointer, or {@code -1}. */
    int itemAt(double mouseX, double mouseY);

    /** Returns the tooltip for the row currently under the pointer, or {@code null}. */
    Component hoveredTooltip();
}
