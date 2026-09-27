package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Smooth vertical single-selection list with any number of real toggles per row. */
public interface KineticMultiToggleList extends KineticControl {
    void setItems(List<? extends MultiToggleItem> items);

    List<MultiToggleItem> items();

    int selectedIndex();

    void setSelectedIndex(int index);

    boolean toggleValue(int rowIndex, int toggleIndex);

    void setToggleValue(int rowIndex, int toggleIndex, boolean value);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    void ensureSelectedVisible();

    int itemAt(double mouseX, double mouseY);

    ToggleHit toggleAt(double mouseX, double mouseY);

    Component hoveredTooltip();
}
