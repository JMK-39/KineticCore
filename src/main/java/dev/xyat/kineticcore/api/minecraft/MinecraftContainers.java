package dev.xyat.kineticcore.api.minecraft;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/**
 * Public Kinetic API for minecraft containers.
 */
public final class MinecraftContainers {
    private MinecraftContainers() {
    }

    /** Extension contract implemented by the container-screen mixin without exposing the mixin package to the API. */
    public interface Access {
        Slot kineticcore$getHoveredSlot();
        int kineticcore$getLeftPos();
        int kineticcore$getTopPos();
    }

    /** Performs a client inventory-slot click and reports whether it was sent. */
    public static boolean clickSlot(int containerId, int slotId, int button, ClickType clickType) {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.clickInventorySlot(containerId, slotId, button, clickType);
    }

    /** Returns the slot currently hovered by the supplied container screen. */
    public static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getHoveredSlot();
    }

    /** Returns the left edge of the supplied container screen. */
    public static int left(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getLeftPos();
    }

    /** Returns the top edge of the supplied container screen. */
    public static int top(AbstractContainerScreen<?> screen) {
        return ((Access) screen).kineticcore$getTopPos();
    }
}
