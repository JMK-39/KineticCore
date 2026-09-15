package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.loot.event.KineticLootEvents;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticLootEventRuntime {
    private static final EnumMap<KineticLootEvents.Priority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> LISTENERS = buckets();
    private static boolean initialized;

    private KineticLootEventRuntime() {
    }

    public static HookRegistration register(KineticLootEvents.Priority priority, KineticLootEvents.TableLoadHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler> bucket = LISTENERS.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        for (KineticLootEvents.Priority priority : KineticLootEvents.Priority.values()) {
            MinecraftForge.EVENT_BUS.addListener(toForge(priority), (LootTableLoadEvent event) -> dispatch(priority, event));
        }
    }

    private static void dispatch(KineticLootEvents.Priority priority, LootTableLoadEvent event) {
        Context context = new Context(event);
        for (KineticLootEvents.TableLoadHandler handler : LISTENERS.get(priority)) {
            handler.handle(context);
        }
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

    private static EventPriority toForge(KineticLootEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static EnumMap<KineticLootEvents.Priority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> buckets() {
        EnumMap<KineticLootEvents.Priority, CopyOnWriteArrayList<KineticLootEvents.TableLoadHandler>> result = new EnumMap<>(KineticLootEvents.Priority.class);
        for (KineticLootEvents.Priority priority : KineticLootEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
