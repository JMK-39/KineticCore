package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.villager.event.KineticVillagerEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticVillagerEventRuntime {
    private static final EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> VILLAGER = villagerBuckets();
    private static final EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> WANDERER = wandererBuckets();
    private static boolean initialized;

    private KineticVillagerEventRuntime() {
    }

    public static HookRegistration registerVillagerTrades(KineticVillagerEvents.Priority priority, KineticVillagerEvents.VillagerTradesHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler> bucket = VILLAGER.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerWandererTrades(KineticVillagerEvents.Priority priority, KineticVillagerEvents.WandererTradesHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler> bucket = WANDERER.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        for (KineticVillagerEvents.Priority priority : KineticVillagerEvents.Priority.values()) {
            EventPriority forgePriority = toForge(priority);
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (VillagerTradesEvent event) -> dispatchVillager(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (WandererTradesEvent event) -> dispatchWanderer(priority, event));
        }
    }

    private static void dispatchVillager(KineticVillagerEvents.Priority priority, VillagerTradesEvent event) {
        VillagerContext context = new VillagerContext(event);
        for (KineticVillagerEvents.VillagerTradesHandler handler : VILLAGER.get(priority)) {
            handler.handle(context);
        }
    }

    private static void dispatchWanderer(KineticVillagerEvents.Priority priority, WandererTradesEvent event) {
        WandererContext context = new WandererContext(event);
        for (KineticVillagerEvents.WandererTradesHandler handler : WANDERER.get(priority)) {
            handler.handle(context);
        }
    }

    private record VillagerContext(VillagerTradesEvent event) implements KineticVillagerEvents.VillagerTradesContext {
        @Override
        public VillagerProfession profession() {
            return event.getType();
        }

        @Override
        public List<VillagerTrades.ItemListing> trades(int level) {
            return event.getTrades().get(level);
        }
    }

    private record WandererContext(WandererTradesEvent event) implements KineticVillagerEvents.WandererTradesContext {
        @Override
        public List<VillagerTrades.ItemListing> genericTrades() {
            return event.getGenericTrades();
        }

        @Override
        public List<VillagerTrades.ItemListing> rareTrades() {
            return event.getRareTrades();
        }
    }

    private static EventPriority toForge(KineticVillagerEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> villagerBuckets() {
        EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> result = new EnumMap<>(KineticVillagerEvents.Priority.class);
        for (KineticVillagerEvents.Priority priority : KineticVillagerEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> wandererBuckets() {
        EnumMap<KineticVillagerEvents.Priority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> result = new EnumMap<>(KineticVillagerEvents.Priority.class);
        for (KineticVillagerEvents.Priority priority : KineticVillagerEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
