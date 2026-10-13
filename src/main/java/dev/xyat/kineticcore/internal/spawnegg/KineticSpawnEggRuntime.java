package dev.xyat.kineticcore.internal.spawnegg;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.spawnegg.KineticSpawnEggs;
import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class KineticSpawnEggRuntime {
    private static final CopyOnWriteArrayList<Entry> HANDLERS = new CopyOnWriteArrayList<>();
    private static final class Entry {
        final KineticEventPriority priority;
        final KineticSpawnEggs.SpawnedHandler handler;
        Entry(KineticEventPriority priority, KineticSpawnEggs.SpawnedHandler handler) { this.priority = priority; this.handler = handler; }
    }
    private KineticSpawnEggRuntime() { }
    public static KineticEventSubscription register(KineticEventPriority priority, KineticSpawnEggs.SpawnedHandler handler) {
        Entry entry = new Entry(Objects.requireNonNull(priority), Objects.requireNonNull(handler));
        HANDLERS.add(entry);
        return KineticEventSubscription.once(() -> HANDLERS.remove(entry));
    }
    public static void dispatch(KineticSpawnEggs.SpawnedContext context) {
        var snapshot = HANDLERS.stream().sorted(Comparator.comparing(entry -> entry.priority)).toList();
        KineticCallbackBatch.runAll(snapshot, entry -> entry.handler.handle(context));
    }
    public static void fire(Entity entity, ItemStack stack, Player player) {
        if (entity.isRemoved() || !(entity.level() instanceof net.minecraft.server.level.ServerLevel level)
                || level.getEntity(entity.getUUID()) != entity) return;
        ItemStack source = stack.copy();
        dispatch(new KineticSpawnEggs.SpawnedContext() {
            public Entity entity() { return entity; }
            public ItemStack stack() { return source.copy(); }
            public Player player() { return player; }
        });
    }
}
