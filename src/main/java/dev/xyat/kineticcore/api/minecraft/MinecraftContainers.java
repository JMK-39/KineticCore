package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public final class MinecraftContainers {
    private MinecraftContainers() {
    }

    public interface Access {
        Slot kineticcore$getHoveredSlot();

        int kineticcore$getLeftPos();

        int kineticcore$getTopPos();
    }

    public static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getHoveredSlot();
    }

    public static int left(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getLeftPos();
    }

    public static int top(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getTopPos();
    }
}
