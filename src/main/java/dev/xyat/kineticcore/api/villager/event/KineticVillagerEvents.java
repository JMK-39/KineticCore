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
    /** The trade pool of one villager profession is being built. Fired once per profession when data loads. */
    public interface VillagerTradesContext {
        /** Returns the profession whose trades are built. */
        VillagerProfession profession();

        /**
         * Returns the live trade list of one level; add or remove listings to change the pool.
         *
         * @param level villager level from 1 (novice) to 5 (master)
         */
        List<VillagerTrades.ItemListing> trades(int level);
    }

    /** The wandering trader's trade pool is being built. */
    public interface WandererTradesContext {
        /** Returns the live list of common trades; add or remove listings to change the pool. */
        List<VillagerTrades.ItemListing> genericTrades();

        /** Returns the live list of rare trades; add or remove listings to change the pool. */
        List<VillagerTrades.ItemListing> rareTrades();
    }

    /** Callback contract for villager trades notifications. */
    @FunctionalInterface
    public interface VillagerTradesHandler {
        /** Called once per profession when trades are built. */
        void handle(VillagerTradesContext context);
    }

    /** Callback contract for wanderer trades notifications. */
    @FunctionalInterface
    public interface WandererTradesHandler {
        /** Called when the wandering trader's trades are built. */
        void handle(WandererTradesContext context);
    }

    private KineticVillagerEvents() {
    }

    /**
     * 注册村民交易监听器；同优先级的回调独立执行，异常在分发结束后报告。
     *
     * <p>Subscribes to villager trade pool building ({@code VillagerTradesEvent}). Callbacks run independently;
     * failures are reported after dispatch.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onVillagerTrades(KineticEventPriority priority, VillagerTradesHandler handler) {
        return KineticVillagerEventRuntime.registerVillagerTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * 注册流浪商人交易监听器；同优先级回调逐项执行，异常在分发结束后报告。
     *
     * <p>Subscribes to wandering trader trade pool building ({@code WandererTradesEvent}). Callbacks run one by
     * one; failures are reported after dispatch.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onWandererTrades(KineticEventPriority priority, WandererTradesHandler handler) {
        return KineticVillagerEventRuntime.registerWandererTrades(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
