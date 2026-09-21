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
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException
 * 时仍执行后续回调，结束后抛出首个异常并附加后续异常。
 * 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 */
public final class KineticWorldEvents {
    /** Callback contract for level notifications. */
    @FunctionalInterface
    public interface LevelHandler {
        void handle(LevelAccessor level);
    }

    /** Context exposed to entity join callbacks. */
    public interface EntityJoinContext {
        Entity entity();

        LevelAccessor level();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for entity join notifications. */
    @FunctionalInterface
    public interface EntityJoinHandler {
        void handle(EntityJoinContext context);
    }

    /** Callback contract for entity leave notifications. */
    @FunctionalInterface
    public interface EntityLeaveHandler {
        void handle(Entity entity, LevelAccessor level);
    }

    /** Context exposed to chunk callbacks. */
    public interface ChunkContext {
        LevelAccessor level();

        ChunkAccess chunk();

        boolean newChunk();
    }

    /** Callback contract for chunk notifications. */
    @FunctionalInterface
    public interface ChunkHandler {
        void handle(ChunkContext context);
    }

    /** Context exposed to block break callbacks. */
    public interface BlockBreakContext {
        LevelAccessor level();

        BlockPos pos();

        BlockState state();

        Player player();

        int experienceToDrop();

        void experienceToDrop(int experience);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for block break notifications. */
    @FunctionalInterface
    public interface BlockBreakHandler {
        void handle(BlockBreakContext context);
    }

    /** Context exposed to block place callbacks. */
    public interface BlockPlaceContext {
        LevelAccessor level();

        BlockPos pos();

        BlockState state();

        Entity entity();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for block place notifications. */
    @FunctionalInterface
    public interface BlockPlaceHandler {
        void handle(BlockPlaceContext context);
    }

    /** Context exposed to farmland trample callbacks. */
    public interface FarmlandTrampleContext {
        LevelAccessor level();

        BlockPos pos();

        BlockState state();

        Entity entity();

        float fallDistance();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for farmland trample notifications. */
    @FunctionalInterface
    public interface FarmlandTrampleHandler {
        void handle(FarmlandTrampleContext context);
    }

    /** Context exposed to item pickup callbacks. */
    public interface ItemPickupContext {
        Player player();

        ItemEntity item();

        ItemStack stack();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for item pickup notifications. */
    @FunctionalInterface
    public interface ItemPickupHandler {
        void handle(ItemPickupContext context);
    }

    /** Supported spawn placement result values exposed by this API. */
    public enum SpawnPlacementResult {
        DEFAULT,
        ALLOW,
        DENY
    }

    /** Context exposed to mob spawn placement callbacks. */
    public interface MobSpawnPlacementContext {
        ServerLevel level();

        EntityType<?> entityType();

        MobSpawnType spawnType();

        BlockPos pos();

        SpawnPlacementResult result();

        void result(SpawnPlacementResult result);
    }

    /** Callback contract for mob spawn placement notifications. */
    @FunctionalInterface
    public interface MobSpawnPlacementHandler {
        void handle(MobSpawnPlacementContext context);
    }

    /** Context exposed to mob finalize spawn callbacks. */
    public interface MobFinalizeSpawnContext {
        LivingEntity entity();

        LevelAccessor level();

        ServerLevel serverLevel();

        MobSpawnType spawnType();

        boolean cancelled();

        void cancel();

        void cancelSpawn();
    }

    /** Callback contract for mob finalize spawn notifications. */
    @FunctionalInterface
    public interface MobFinalizeSpawnHandler {
        void handle(MobFinalizeSpawnContext context);
    }

    /** Context exposed to baby spawn callbacks. */
    public interface BabySpawnContext {
        LivingEntity parentA();

        LivingEntity parentB();

        LivingEntity child();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for baby spawn notifications. */
    @FunctionalInterface
    public interface BabySpawnHandler {
        void handle(BabySpawnContext context);
    }

    private KineticWorldEvents() {
    }

    /**
     * Registers a listener for level load.
     */
    public static KineticEventSubscription onLevelLoad(KineticEventPriority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for level unload.
     */
    public static KineticEventSubscription onLevelUnload(KineticEventPriority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for entity join.
     */
    public static KineticEventSubscription onEntityJoin(KineticEventPriority priority, EntityJoinHandler handler) {
        return KineticWorldEventRuntime.registerEntityJoin(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for entity leave.
     */
    public static KineticEventSubscription onEntityLeave(KineticEventPriority priority, EntityLeaveHandler handler) {
        return KineticWorldEventRuntime.registerEntityLeave(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for chunk load.
     */
    public static KineticEventSubscription onChunkLoad(KineticEventPriority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for chunk unload.
     */
    public static KineticEventSubscription onChunkUnload(KineticEventPriority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for block break.
     */
    public static KineticEventSubscription onBlockBreak(KineticEventPriority priority, BlockBreakHandler handler) {
        return KineticWorldEventRuntime.registerBlockBreak(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for block place.
     */
    public static KineticEventSubscription onBlockPlace(KineticEventPriority priority, BlockPlaceHandler handler) {
        return KineticWorldEventRuntime.registerBlockPlace(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for farmland trample.
     */
    public static KineticEventSubscription onFarmlandTrample(KineticEventPriority priority, FarmlandTrampleHandler handler) {
        return KineticWorldEventRuntime.registerFarmlandTrample(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for item pickup.
     */
    public static KineticEventSubscription onItemPickup(KineticEventPriority priority, ItemPickupHandler handler) {
        return KineticWorldEventRuntime.registerItemPickup(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for mob spawn placement check.
     */
    public static KineticEventSubscription onMobSpawnPlacementCheck(KineticEventPriority priority, MobSpawnPlacementHandler handler) {
        return KineticWorldEventRuntime.registerMobSpawnPlacementCheck(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for mob finalize spawn.
     */
    public static KineticEventSubscription onMobFinalizeSpawn(KineticEventPriority priority, MobFinalizeSpawnHandler handler) {
        return KineticWorldEventRuntime.registerMobFinalizeSpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    /**
     * Registers a listener for baby spawn.
     */
    public static KineticEventSubscription onBabySpawn(KineticEventPriority priority, BabySpawnHandler handler) {
        return KineticWorldEventRuntime.registerBabySpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static KineticEventPriority require(KineticEventPriority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
