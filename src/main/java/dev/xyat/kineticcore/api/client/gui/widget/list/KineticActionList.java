package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Smooth vertical single-selection list with one standard trailing action button per row.
 */
public interface KineticActionList extends KineticControl {
    void setItems(List<? extends ActionItem> items);

    List<ActionItem> items();

    int selectedIndex();

    void setSelectedIndex(int index);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    void ensureSelectedVisible();

    int itemAt(double mouseX, double mouseY);

    int actionAt(double mouseX, double mouseY);

    Component hoveredTooltip();
}
