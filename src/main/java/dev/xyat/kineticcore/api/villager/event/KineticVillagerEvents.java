package dev.xyat.kineticcore.api.villager.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticVillagerEventRuntime;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.List;
import java.util.Objects;

/** Public Kinetic API facade for villager events. */
public final class KineticVillagerEvents {
    /** Context exposed to villager trades callbacks. */
    public interface VillagerTradesContext {
        VillagerProfession profession();

        List<VillagerTrades.ItemListing> trades(int level);
    }

    /** Context exposed to wanderer trades callbacks. */
    public interface WandererTradesContext {
        List<VillagerTrades.ItemListing> genericTrades();

        List<VillagerTrades.ItemListing> rareTrades();
    }

    /** Callback contract for villager trades notifications. */
    @FunctionalInterface
    public interface VillagerTradesHandler {
        void handle(VillagerTradesContext context);
    }

    /** Callback contract for wanderer trades notifications. */
    @FunctionalInterface
    public interface WandererTradesHandler {
        void handle(WandererTradesContext context);
    }

    private KineticVillagerEvents() {
    }

    /**
     * 注册村民交易监听器；同优先级的回调独立执行，异常在分发结束后报告。
     */
    public static KineticEventSubscription onVillagerTrades(KineticEventPriority priority, VillagerTradesHandler handler) {
        return KineticVillagerEventRuntime.registerVillagerTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * 注册流浪商人交易监听器；同优先级回调逐项执行，异常在分发结束后报告。
     */
    public static KineticEventSubscription onWandererTrades(KineticEventPriority priority, WandererTradesHandler handler) {
        return KineticVillagerEventRuntime.registerWandererTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
