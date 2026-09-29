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
     * Returns whether a menu slot shows the given player's own inventory (hotbar, main inventory or armor),
     * including slots backed by Forge item handler wrappers.
     *
     * @throws NullPointerException if an argument is {@code null}
     */
    public static boolean isPlayerInventorySlot(Slot slot, Player player) {
        return KineticInventoryRuntime.isPlayerInventorySlot(
                Objects.requireNonNull(slot, "slot"),
                Objects.requireNonNull(player, "player")
        );
    }

    /** Returns whether a Forge item handler wraps a player inventory; {@code null} returns {@code false}. */
    public static boolean isPlayerInventoryHandler(Object handler) {
        return KineticInventoryRuntime.isPlayerInventoryHandler(handler);
    }
}
