package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Smooth vertical single-selection list with a standard item slot in every row. */
public interface KineticItemSelectionList extends KineticControl {
    void setItems(List<? extends ItemSelectionItem> items);

    List<ItemSelectionItem> items();

    int selectedIndex();

    void setSelectedIndex(int index);

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    void ensureSelectedVisible();

    int itemAt(double mouseX, double mouseY);

    ItemStack stackAt(double mouseX, double mouseY);

    ItemStack hoveredStack();

    Component hoveredTooltip();
}
