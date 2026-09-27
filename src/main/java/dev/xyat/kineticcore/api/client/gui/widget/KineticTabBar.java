package dev.xyat.kineticcore.api.client.gui.widget;



/** Standard equal-width tab bar managed by the Kinetic widget API. */
public interface KineticTabBar {
    /** Returns the tab index currently under the pointer, or {@code -1}. */
    int tabAt(double mouseX, double mouseY);

    /** Returns the currently selected tab index, or {@code -1} when no tabs exist. */
    int selectedIndex();

    /** Synchronizes the selected tab without invoking the selection responder. */
    void setSelectedIndex(int index);

    /** Enables or disables one tab while keeping selection rendering API-managed. */
    void setTabActive(int index, boolean active);
}
