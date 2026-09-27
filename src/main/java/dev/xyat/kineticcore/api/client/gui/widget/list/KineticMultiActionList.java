package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Smooth vertical single-selection list with any number of standard trailing row actions. */
public interface KineticMultiActionList extends KineticControl {
    void setItems(List<? extends MultiActionItem> items);

    List<MultiActionItem> items();

    int selectedIndex();

    void setSelectedIndex(int index);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    void ensureSelectedVisible();

    int itemAt(double mouseX, double mouseY);

    ActionHit actionAt(double mouseX, double mouseY);

    Component hoveredTooltip();
}
