package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Smooth vertical multi-toggle list using standard Kinetic state buttons, clipping, and scrollbar behavior.
 */
public interface KineticToggleList extends KineticControl {
    void setItems(List<? extends ToggleItem> items);

    List<ToggleItem> items();

    boolean value(int index);

    void setValue(int index, boolean value);

    void setValues(List<Boolean> values);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    int itemAt(double mouseX, double mouseY);

    Component hoveredTooltip();
}
