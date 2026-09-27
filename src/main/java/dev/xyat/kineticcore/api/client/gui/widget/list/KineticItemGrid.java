package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Smooth scrollable item-slot grid with API-defined density and caller-owned selection semantics. */
public interface KineticItemGrid extends KineticControl {
    void setItems(List<? extends ItemGridItem> items);

    List<ItemGridItem> items();

    void setBounds(int x, int y, int width, int height);

    int scrollOffset();

    void setScrollOffset(int offset);

    int maxScrollOffset();

    int columns();

    int visibleRows();

    int itemAt(double mouseX, double mouseY);

    ItemStack stackAt(double mouseX, double mouseY);

    ItemStack hoveredStack();

    Component hoveredTooltip();
}
