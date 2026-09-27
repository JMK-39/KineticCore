package dev.xyat.kineticcore.internal.client.gui.page;

import net.minecraft.world.inventory.Slot;

/** Container-specific host services for container pages. */
public interface ContainerPageHost extends PageHost {
    Slot hoveredSlot();

    int leftPos();

    int topPos();

    int imageWidth();

    int imageHeight();

    void setImageSize(int width, int height);

    void setTitleLabelPosition(int x, int y);

    void setInventoryLabelPosition(int x, int y);
}
