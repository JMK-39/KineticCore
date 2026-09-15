package dev.xyat.kineticcore.api.villager.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.runtime.event.KineticVillagerEventRuntime;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.List;
import java.util.Objects;

public final class KineticVillagerEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    public interface VillagerTradesContext {
        VillagerProfession profession();

        List<VillagerTrades.ItemListing> trades(int level);
    }

    public interface WandererTradesContext {
        List<VillagerTrades.ItemListing> genericTrades();

        List<VillagerTrades.ItemListing> rareTrades();
    }

    @FunctionalInterface
    public interface VillagerTradesHandler {
        void handle(VillagerTradesContext context);
    }

    @FunctionalInterface
    public interface WandererTradesHandler {
        void handle(WandererTradesContext context);
    }

    private KineticVillagerEvents() {
    }

    public static HookRegistration onVillagerTrades(VillagerTradesHandler handler) {
        return onVillagerTrades(Priority.NORMAL, handler);
    }

    public static HookRegistration onVillagerTrades(Priority priority, VillagerTradesHandler handler) {
        return KineticVillagerEventRuntime.registerVillagerTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onWandererTrades(WandererTradesHandler handler) {
        return onWandererTrades(Priority.NORMAL, handler);
    }

    public static HookRegistration onWandererTrades(Priority priority, WandererTradesHandler handler) {
        return KineticVillagerEventRuntime.registerWandererTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
