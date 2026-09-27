package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Smooth vertical single-selection list with one real toggle and one trailing action per row. */
public interface KineticToggleActionList extends KineticControl {
    void setItems(List<? extends ToggleActionItem> items);

    List<ToggleActionItem> items();

    int selectedIndex();

    void setSelectedIndex(int index);

    boolean toggleValue(int index);

    void setToggleValue(int index, boolean value);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    void ensureSelectedVisible();

    int itemAt(double mouseX, double mouseY);

    int toggleAt(double mouseX, double mouseY);

    int actionAt(double mouseX, double mouseY);

    Component hoveredTooltip();
}
