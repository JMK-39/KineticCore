package dev.xyat.kineticcore.api.world.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticWorldEventRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Objects;

/**
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException 时仍执行后续回调，结束后抛出首个异常并附加后续异常。 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 *
 * <p>Event subscriptions. Non-cancellable callbacks run one by one in registration order; when one throws a
 * RuntimeException the rest still run, and the first exception is rethrown afterwards with later ones attached as
 * suppressed. Cancellable callbacks isolate exceptions the same way: while the event is not cancelled the next
 * handler runs; once it is cancelled dispatch stops, even if the cancelling handler then throws, and the first
 * exception and later errors are reported at the end.
 */
public final class KineticWorldEvents {
    /** Callback contract for level notifications. */
    @FunctionalInterface
    public interface LevelHandler {
        /**
         * Called for the level that was loaded or unloaded, on both logical sides; check
         * {@code level.isClientSide()} when only one side matters.
         */
        void handle(LevelAccessor level);
    }

    /** An entity is being added to a level, including when its chunk loads from disk. */
    public interface EntityJoinContext {
        /** Returns the entity being added. */
        Entity entity();

        /** Returns the level the entity joins. */
        LevelAccessor level();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the entity is not added to the level. For entities loaded from disk this discards
         * them. Later Kinetic handlers of this event are skipped.
         */
        void cancel();
    }

    /** Callback contract for entity join notifications. */
    @FunctionalInterface
    public interface EntityJoinHandler {
        /**
         * Called on the thread of the logical side that fired the event; check {@code level().isClientSide()} when
         * only one side matters.
         */
        void handle(EntityJoinContext context);
    }

    /** Callback contract for entity leave notifications. */
    @FunctionalInterface
    public interface EntityLeaveHandler {
        /**
         * Called after an entity was removed from a level, including when its chunk unloads, on both logical sides.
         */
        void handle(Entity entity, LevelAccessor level);
    }

    /** A chunk was loaded or unloaded. */
    public interface ChunkContext {
        /** Returns the level of the chunk. */
        LevelAccessor level();

        /** Returns the chunk. During unload it is about to become unavailable; do not keep a reference. */
        ChunkAccess chunk();

        /**
         * Returns whether the chunk was generated for the first time; always {@code false} for unloads and on the
         * client.
         */
        boolean newChunk();
    }

    /** Callback contract for chunk notifications. */
    @FunctionalInterface
    public interface ChunkHandler {
        /**
         * Called on the thread of the logical side that fired the event; check {@code level().isClientSide()} when
         * only one side matters. Chunk events are frequent, so keep handlers cheap.
         */
        void handle(ChunkContext context);
    }

    /** A player is about to break a block. Fired on the server only. */
    public interface BlockBreakContext {
        /** Returns the level of the block. */
        LevelAccessor level();

        /** Returns the position of the block. */
        BlockPos pos();

        /** Returns the block state being broken. */
        BlockState state();

        /** Returns the player breaking the block. */
        Player player();

        /** Returns the experience the block will drop, for example from ores. */
        int experienceToDrop();

        /** Replaces the experience the block will drop. */
        void experienceToDrop(int experience);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the block stays and nothing drops. Later Kinetic handlers of this event are skipped.
         */
        void cancel();
    }

    /** Callback contract for block break notifications. */
    @FunctionalInterface
    public interface BlockBreakHandler {
        /** Called on the server thread. */
        void handle(BlockBreakContext context);
    }

    /** An entity placed a block. Fired on the server only, after the block was set. */
    public interface BlockPlaceContext {
        /** Returns the level of the block. */
        LevelAccessor level();

        /** Returns the position of the placed block. */
        BlockPos pos();

        /** Returns the placed block state. */
        BlockState state();

        /** Returns the entity that placed the block, or {@code null} when unknown. */
        Entity entity();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the placement is reverted and the item is not consumed. Later Kinetic handlers of this
         * event are skipped.
         */
        void cancel();
    }

    /** Callback contract for block place notifications. */
    @FunctionalInterface
    public interface BlockPlaceHandler {
        /** Called on the server thread. */
        void handle(BlockPlaceContext context);
    }

    /** An entity is about to trample farmland into dirt. */
    public interface FarmlandTrampleContext {
        /** Returns the level of the farmland. */
        LevelAccessor level();

        /** Returns the position of the farmland. */
        BlockPos pos();

        /** Returns the farmland block state. */
        BlockState state();

        /** Returns the entity that landed on the farmland. */
        Entity entity();

        /** Returns the fall distance that caused the trample. */
        float fallDistance();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the farmland stays. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for farmland trample notifications. */
    @FunctionalInterface
    public interface FarmlandTrampleHandler {
        /** Called on the server thread. */
        void handle(FarmlandTrampleContext context);
    }

    /** A player is about to pick up an item entity. */
    public interface ItemPickupContext {
        /** Returns the player picking up the item. */
        Player player();

        /** Returns the item entity on the ground. */
        ItemEntity item();

        /** Returns the stack inside the item entity; changes affect what is picked up. */
        ItemStack stack();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the item stays on the ground. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for item pickup notifications. */
    @FunctionalInterface
    public interface ItemPickupHandler {
        /** Called on the server thread. */
        void handle(ItemPickupContext context);
    }

    /** Result of a mob spawn placement check. */
    public enum SpawnPlacementResult {
        /** Let vanilla spawn rules decide. */
        DEFAULT,
        /** Allow the spawn at this position even if vanilla rules would refuse it. */
        ALLOW,
        /** Refuse the spawn at this position. */
        DENY
    }

    /** The game checks whether a mob type may spawn at a position, before the mob is created. */
    public interface MobSpawnPlacementContext {
        /** Returns the level the mob would spawn in. */
        ServerLevel level();

        /** Returns the type of mob being checked. */
        EntityType<?> entityType();

        /** Returns why the mob is spawning, for example natural spawning or a spawner. */
        MobSpawnType spawnType();

        /** Returns the candidate position. */
        BlockPos pos();

        /** Returns the current decision; {@link SpawnPlacementResult#DEFAULT} until a handler changes it. */
        SpawnPlacementResult result();

        /** Sets the decision; the last handler to set it wins. */
        void result(SpawnPlacementResult result);
    }

    /** Callback contract for mob spawn placement notifications. */
    @FunctionalInterface
    public interface MobSpawnPlacementHandler {
        /** Called on the server thread. */
        void handle(MobSpawnPlacementContext context);
    }

    /** A mob was created and is about to be initialized (equipment, variants) and added to the world. */
    public interface MobFinalizeSpawnContext {
        /** Returns the mob being spawned. */
        LivingEntity entity();

        /** Returns the level accessor used for the spawn; may be a world-generation region. */
        LevelAccessor level();

        /** Returns the server level the mob spawns in. */
        ServerLevel serverLevel();

        /** Returns why the mob is spawning. */
        MobSpawnType spawnType();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: the mob is not added to the world and is not initialized. Later Kinetic handlers of
         * this event are skipped.
         */
        void cancel();

        /**
         * Prevents the mob from being added to the world without cancelling the event, so later handlers still run.
         */
        void cancelSpawn();
    }

    /** Callback contract for mob finalize spawn notifications. */
    @FunctionalInterface
    public interface MobFinalizeSpawnHandler {
        /** Called on the server thread. */
        void handle(MobFinalizeSpawnContext context);
    }

    /** Two animals are about to produce a baby. */
    public interface BabySpawnContext {
        /** Returns the first parent. */
        LivingEntity parentA();

        /** Returns the second parent. */
        LivingEntity parentB();

        /** Returns the baby that will be spawned, or {@code null} when none will be. */
        LivingEntity child();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: no baby is spawned. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for baby spawn notifications. */
    @FunctionalInterface
    public interface BabySpawnHandler {
        /** Called on the server thread. */
        void handle(BabySpawnContext context);
    }

    private KineticWorldEvents() {
    }

    /**
     * Subscribes to level loading ({@code LevelEvent.Load}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onLevelLoad(KineticEventPriority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to level unloading ({@code LevelEvent.Unload}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onLevelUnload(KineticEventPriority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to entities joining a level ({@code EntityJoinLevelEvent}). The Forge listener for a priority is
     * installed on its first subscription, so ordering relative to other mods' Forge listeners of the same priority
     * follows registration order.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onEntityJoin(KineticEventPriority priority, EntityJoinHandler handler) {
        return KineticWorldEventRuntime.registerEntityJoin(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to entities leaving a level ({@code EntityLeaveLevelEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onEntityLeave(KineticEventPriority priority, EntityLeaveHandler handler) {
        return KineticWorldEventRuntime.registerEntityLeave(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to chunk loading ({@code ChunkEvent.Load}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onChunkLoad(KineticEventPriority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to chunk unloading ({@code ChunkEvent.Unload}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onChunkUnload(KineticEventPriority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to blocks broken by players ({@code BlockEvent.BreakEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onBlockBreak(KineticEventPriority priority, BlockBreakHandler handler) {
        return KineticWorldEventRuntime.registerBlockBreak(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to blocks placed by entities ({@code BlockEvent.EntityPlaceEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onBlockPlace(KineticEventPriority priority, BlockPlaceHandler handler) {
        return KineticWorldEventRuntime.registerBlockPlace(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to farmland trampling ({@code BlockEvent.FarmlandTrampleEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onFarmlandTrample(KineticEventPriority priority, FarmlandTrampleHandler handler) {
        return KineticWorldEventRuntime.registerFarmlandTrample(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to item pickup by players ({@code EntityItemPickupEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onItemPickup(KineticEventPriority priority, ItemPickupHandler handler) {
        return KineticWorldEventRuntime.registerItemPickup(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to mob spawn placement checks ({@code MobSpawnEvent.SpawnPlacementCheck}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onMobSpawnPlacementCheck(KineticEventPriority priority, MobSpawnPlacementHandler handler) {
        return KineticWorldEventRuntime.registerMobSpawnPlacementCheck(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to mob spawn finalization ({@code MobSpawnEvent.FinalizeSpawn}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onMobFinalizeSpawn(KineticEventPriority priority, MobFinalizeSpawnHandler handler) {
        return KineticWorldEventRuntime.registerMobFinalizeSpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Subscribes to animal breeding ({@code BabyEntitySpawnEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onBabySpawn(KineticEventPriority priority, BabySpawnHandler handler) {
        return KineticWorldEventRuntime.registerBabySpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static KineticEventPriority require(KineticEventPriority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
