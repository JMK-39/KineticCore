package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.loot.event.KineticLootEvents;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticLootEventRuntime {
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> LISTENERS = buckets();
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticLootEventRuntime() {
    }

    public static KineticEventSubscription register(KineticEventPriority priority, KineticLootEvents.TableLoadHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler> bucket = LISTENERS.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    private static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(toForge(priority), (LootTableLoadEvent event) -> dispatch(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    private static void dispatch(KineticEventPriority priority, LootTableLoadEvent event) {
        Context context = new Context(event);
        KineticCallbackBatch.runAll(LISTENERS.get(priority), handler -> handler.handle(context));
    }

    private record Context(LootTableLoadEvent event) implements KineticLootEvents.TableLoadContext {
        @Override
        public net.minecraft.resources.ResourceLocation id() {
            return event.getName();
        }

        @Override
        public LootTable table() {
            return event.getTable();
        }

        @Override
        public void table(LootTable table) {
            event.setTable(table);
        }
    }

    private static EventPriority toForge(KineticEventPriority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> buckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
