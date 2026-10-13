package dev.xyat.kineticcore.api.spawnegg;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.spawnegg.KineticSpawnEggRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Synchronous notifications after a thrown egg finishes spawning and applying its entity data. */
public final class KineticSpawnEggs {
    /** Completed spawn; no cancellation or deferred execution is involved. */
    public interface SpawnedContext {
        /** Returns the live spawned entity on the server. */
        Entity entity();
        /** Returns a fresh defensive copy of the source egg. */
        ItemStack stack();
        /** Returns the throwing player, or null for a dispenser or non-player owner. */
        Player player();
    }
    /** Callback invoked on the spawning server thread. */
    @FunctionalInterface
    public interface SpawnedHandler {
        /** Handles one successful thrown-egg spawn. */
        void handle(SpawnedContext context);
    }
    private KineticSpawnEggs() { }
    /**
     * Subscribes in priority order, then registration order within a priority.
     * The entity's synchronous spawn/join listeners and egg NBT have already completed.
     * Future ticks or async changes by other mods are outside this notification's ordering guarantee.
     * @param priority dispatch priority
     * @param handler callback
     * @return handle whose close removes this registration
     */
    public static KineticEventSubscription onSpawned(KineticEventPriority priority, SpawnedHandler handler) {
        return KineticSpawnEggRuntime.register(priority, handler);
    }
    /**
     * Publishes completion from a spawn-egg integration, synchronously on the server thread.
     * @param entity successfully spawned entity
     * @param stack source egg
     * @param player owner, or null for a dispenser
     */
    public static void notifySpawned(Entity entity, ItemStack stack, Player player) {
        KineticSpawnEggRuntime.fire(entity, stack, player);
    }
}
