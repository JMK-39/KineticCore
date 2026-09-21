package dev.xyat.kineticcore.api.inventory;

import dev.xyat.kineticcore.internal.inventory.KineticInventoryRuntime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

import java.util.Objects;

/** Public Kinetic API facade for inventory slots. */
public final class KineticInventorySlots {
    private KineticInventorySlots() {
    }

    /**
     * Returns whether player inventory slot.
     */
    public static boolean isPlayerInventorySlot(Slot slot, Player player) {
        return KineticInventoryRuntime.isPlayerInventorySlot(
                Objects.requireNonNull(slot, "slot"),
                Objects.requireNonNull(player, "player")
        );
    }

    /**
     * Returns whether player inventory handler.
     */
    public static boolean isPlayerInventoryHandler(Object handler) {
        return KineticInventoryRuntime.isPlayerInventoryHandler(handler);
    }
}
