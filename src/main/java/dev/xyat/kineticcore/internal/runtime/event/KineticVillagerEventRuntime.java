package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;
import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
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
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> VILLAGER = villagerBuckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> WANDERER = wandererBuckets();
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticVillagerEventRuntime() {
    }

    public static KineticEventSubscription registerVillagerTrades(KineticEventPriority priority, KineticVillagerEvents.VillagerTradesHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler> bucket = VILLAGER.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerWandererTrades(KineticEventPriority priority, KineticVillagerEvents.WandererTradesHandler handler) {
        initialize();
        CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler> bucket = WANDERER.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    private static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            EventPriority forgePriority = toForge(priority);
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (VillagerTradesEvent event) -> dispatchVillager(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (WandererTradesEvent event) -> dispatchWanderer(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    private static void dispatchVillager(KineticEventPriority priority, VillagerTradesEvent event) {
        VillagerContext context = new VillagerContext(event);
        KineticCallbackBatch.runAll(VILLAGER.get(priority), handler -> handler.handle(context));
    }

    private static void dispatchWanderer(KineticEventPriority priority, WandererTradesEvent event) {
        WandererContext context = new WandererContext(event);
        KineticCallbackBatch.runAll(WANDERER.get(priority), handler -> handler.handle(context));
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

    private static EventPriority toForge(KineticEventPriority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> villagerBuckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.VillagerTradesHandler>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> wandererBuckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticVillagerEvents.WandererTradesHandler>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
