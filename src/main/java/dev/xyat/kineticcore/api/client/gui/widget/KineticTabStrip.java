package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Compact variable-width tab strip with optional pinned leading tabs, horizontal scrolling,
 * previous/next controls, clipping, smooth wheel scrolling, and a themed scrollbar.
 */
public interface KineticTabStrip extends KineticControl {

    /** Replaces the complete tab model while preserving API-managed layout and scrolling. */
    void setTabs(List<? extends TabStripItem> tabs);

    /** Returns the immutable tab model currently displayed by this strip. */
    List<TabStripItem> tabs();

    /** Returns the currently selected tab index, or {@code -1} when no tabs exist. */
    int selectedIndex();

    /** Synchronizes the selected tab without invoking the selection responder. */
    void setSelectedIndex(int index);

    /** Returns the nearest current horizontal pixel offset of the scrollable portion. */
    int scrollOffset();

    /** Immediately sets the horizontal pixel offset, clamped to the current content range. */
    void setScrollOffset(int offset);

    /** Returns the maximum horizontal pixel offset currently available. */
    int maxScrollOffset();

    /** Scrolls the variable-width portion by a pixel delta. Positive values move content rightward. */
    void scrollBy(int pixels);

    /** Ensures the selected non-pinned tab is fully visible inside the scrolling viewport. */
    void ensureSelectedVisible();

    /** Returns the tab index currently under the pointer, or {@code -1}. */
    int tabAt(double mouseX, double mouseY);

    /** Returns the tooltip for the tab currently under the pointer, or {@code null}. */
    Component hoveredTooltip();
}
