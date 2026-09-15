package dev.xyat.kineticcore.api.world.event;

import dev.xyat.kineticcore.internal.runtime.event.KineticWorldEventRuntime;
import dev.xyat.kineticcore.api.hook.HookRegistration;
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

public final class KineticWorldEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    @FunctionalInterface
    public interface LevelHandler {
        void handle(LevelAccessor level);
    }

    public interface EntityJoinContext {
        Entity entity();

        LevelAccessor level();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface EntityJoinHandler {
        void handle(EntityJoinContext context);
    }

    @FunctionalInterface
    public interface EntityLeaveHandler {
        void handle(Entity entity, LevelAccessor level);
    }

    public interface ChunkContext {
        LevelAccessor level();

        ChunkAccess chunk();

        boolean newChunk();
    }

    @FunctionalInterface
    public interface ChunkHandler {
        void handle(ChunkContext context);
    }

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

    @FunctionalInterface
    public interface BlockBreakHandler {
        void handle(BlockBreakContext context);
    }

    public interface BlockPlaceContext {
        LevelAccessor level();

        BlockPos pos();

        BlockState state();

        Entity entity();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface BlockPlaceHandler {
        void handle(BlockPlaceContext context);
    }

    public interface ItemPickupContext {
        Player player();

        ItemEntity item();

        ItemStack stack();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface ItemPickupHandler {
        void handle(ItemPickupContext context);
    }

    public enum SpawnPlacementResult {
        DEFAULT,
        ALLOW,
        DENY
    }

    public interface MobSpawnPlacementContext {
        ServerLevel level();

        EntityType<?> entityType();

        MobSpawnType spawnType();

        BlockPos pos();

        SpawnPlacementResult result();

        void result(SpawnPlacementResult result);
    }

    @FunctionalInterface
    public interface MobSpawnPlacementHandler {
        void handle(MobSpawnPlacementContext context);
    }

    public interface MobFinalizeSpawnContext {
        LivingEntity entity();

        LevelAccessor level();

        ServerLevel serverLevel();

        MobSpawnType spawnType();

        boolean cancelled();

        void cancel();

        void cancelSpawn();
    }

    @FunctionalInterface
    public interface MobFinalizeSpawnHandler {
        void handle(MobFinalizeSpawnContext context);
    }

    public interface BabySpawnContext {
        LivingEntity parentA();

        LivingEntity parentB();

        LivingEntity child();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface BabySpawnHandler {
        void handle(BabySpawnContext context);
    }

    private KineticWorldEvents() {
    }

    public static HookRegistration onLevelLoad(LevelHandler handler) {
        return onLevelLoad(Priority.NORMAL, handler);
    }

    public static HookRegistration onLevelLoad(Priority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onLevelUnload(LevelHandler handler) {
        return onLevelUnload(Priority.NORMAL, handler);
    }

    public static HookRegistration onLevelUnload(Priority priority, LevelHandler handler) {
        return KineticWorldEventRuntime.registerLevelUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onEntityJoin(EntityJoinHandler handler) {
        return onEntityJoin(Priority.NORMAL, handler);
    }

    public static HookRegistration onEntityJoin(Priority priority, EntityJoinHandler handler) {
        return KineticWorldEventRuntime.registerEntityJoin(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onEntityLeave(EntityLeaveHandler handler) {
        return onEntityLeave(Priority.NORMAL, handler);
    }

    public static HookRegistration onEntityLeave(Priority priority, EntityLeaveHandler handler) {
        return KineticWorldEventRuntime.registerEntityLeave(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onChunkLoad(ChunkHandler handler) {
        return onChunkLoad(Priority.NORMAL, handler);
    }

    public static HookRegistration onChunkLoad(Priority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkLoad(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onChunkUnload(ChunkHandler handler) {
        return onChunkUnload(Priority.NORMAL, handler);
    }

    public static HookRegistration onChunkUnload(Priority priority, ChunkHandler handler) {
        return KineticWorldEventRuntime.registerChunkUnload(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onBlockBreak(BlockBreakHandler handler) {
        return onBlockBreak(Priority.NORMAL, handler);
    }

    public static HookRegistration onBlockBreak(Priority priority, BlockBreakHandler handler) {
        return KineticWorldEventRuntime.registerBlockBreak(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onBlockPlace(BlockPlaceHandler handler) {
        return onBlockPlace(Priority.NORMAL, handler);
    }

    public static HookRegistration onBlockPlace(Priority priority, BlockPlaceHandler handler) {
        return KineticWorldEventRuntime.registerBlockPlace(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onItemPickup(ItemPickupHandler handler) {
        return onItemPickup(Priority.NORMAL, handler);
    }

    public static HookRegistration onItemPickup(Priority priority, ItemPickupHandler handler) {
        return KineticWorldEventRuntime.registerItemPickup(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onMobSpawnPlacementCheck(MobSpawnPlacementHandler handler) {
        return onMobSpawnPlacementCheck(Priority.NORMAL, handler);
    }

    public static HookRegistration onMobSpawnPlacementCheck(Priority priority, MobSpawnPlacementHandler handler) {
        return KineticWorldEventRuntime.registerMobSpawnPlacementCheck(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onMobFinalizeSpawn(MobFinalizeSpawnHandler handler) {
        return onMobFinalizeSpawn(Priority.NORMAL, handler);
    }

    public static HookRegistration onMobFinalizeSpawn(Priority priority, MobFinalizeSpawnHandler handler) {
        return KineticWorldEventRuntime.registerMobFinalizeSpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    public static HookRegistration onBabySpawn(BabySpawnHandler handler) {
        return onBabySpawn(Priority.NORMAL, handler);
    }

    public static HookRegistration onBabySpawn(Priority priority, BabySpawnHandler handler) {
        return KineticWorldEventRuntime.registerBabySpawn(require(priority), Objects.requireNonNull(handler, "handler"));
    }

    private static Priority require(Priority priority) {
        return Objects.requireNonNull(priority, "priority");
    }
}
