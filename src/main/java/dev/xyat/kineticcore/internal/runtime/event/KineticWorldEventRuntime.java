package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.world.event.KineticWorldEvents;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticWorldEventRuntime {
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> LEVEL_LOAD = levelHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> LEVEL_UNLOAD = levelHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.EntityJoinHandler>> ENTITY_JOIN = entityJoinHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.EntityLeaveHandler>> ENTITY_LEAVE = entityLeaveHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> CHUNK_LOAD = chunkHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> CHUNK_UNLOAD = chunkHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BlockBreakHandler>> BLOCK_BREAK = blockBreakHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BlockPlaceHandler>> BLOCK_PLACE = blockPlaceHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.ItemPickupHandler>> ITEM_PICKUP = itemPickupHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.MobSpawnPlacementHandler>> MOB_SPAWN_PLACEMENT = mobSpawnPlacementHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.MobFinalizeSpawnHandler>> MOB_FINALIZE_SPAWN = mobFinalizeSpawnHandlers();
    private static final EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BabySpawnHandler>> BABY_SPAWN = babySpawnHandlers();

    private static boolean initialized;

    private KineticWorldEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        for (KineticWorldEvents.Priority priority : KineticWorldEvents.Priority.values()) {
            EventPriority forgePriority = toForge(priority);
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LevelEvent.Load event) -> onLevelLoad(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (LevelEvent.Unload event) -> onLevelUnload(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityJoinLevelEvent event) -> onEntityJoin(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityLeaveLevelEvent event) -> onEntityLeave(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ChunkEvent.Load event) -> onChunkLoad(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ChunkEvent.Unload event) -> onChunkUnload(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (BlockEvent.BreakEvent event) -> onBlockBreak(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (BlockEvent.EntityPlaceEvent event) -> onBlockPlace(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityItemPickupEvent event) -> onItemPickup(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobSpawnEvent.SpawnPlacementCheck event) -> onMobSpawnPlacementCheck(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobSpawnEvent.FinalizeSpawn event) -> onMobFinalizeSpawn(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (BabyEntitySpawnEvent event) -> onBabySpawn(priority, event));
        }
    }

    public static HookRegistration registerLevelLoad(KineticWorldEvents.Priority priority, KineticWorldEvents.LevelHandler handler) {
        initialize();
        return add(LEVEL_LOAD, priority, handler);
    }

    public static HookRegistration registerLevelUnload(KineticWorldEvents.Priority priority, KineticWorldEvents.LevelHandler handler) {
        initialize();
        return add(LEVEL_UNLOAD, priority, handler);
    }

    public static HookRegistration registerEntityJoin(KineticWorldEvents.Priority priority, KineticWorldEvents.EntityJoinHandler handler) {
        initialize();
        return add(ENTITY_JOIN, priority, handler);
    }

    public static HookRegistration registerEntityLeave(KineticWorldEvents.Priority priority, KineticWorldEvents.EntityLeaveHandler handler) {
        initialize();
        return add(ENTITY_LEAVE, priority, handler);
    }

    public static HookRegistration registerChunkLoad(KineticWorldEvents.Priority priority, KineticWorldEvents.ChunkHandler handler) {
        initialize();
        return add(CHUNK_LOAD, priority, handler);
    }

    public static HookRegistration registerChunkUnload(KineticWorldEvents.Priority priority, KineticWorldEvents.ChunkHandler handler) {
        initialize();
        return add(CHUNK_UNLOAD, priority, handler);
    }

    public static HookRegistration registerBlockBreak(KineticWorldEvents.Priority priority, KineticWorldEvents.BlockBreakHandler handler) {
        initialize();
        return add(BLOCK_BREAK, priority, handler);
    }

    public static HookRegistration registerBlockPlace(KineticWorldEvents.Priority priority, KineticWorldEvents.BlockPlaceHandler handler) {
        initialize();
        return add(BLOCK_PLACE, priority, handler);
    }

    public static HookRegistration registerItemPickup(KineticWorldEvents.Priority priority, KineticWorldEvents.ItemPickupHandler handler) {
        initialize();
        return add(ITEM_PICKUP, priority, handler);
    }

    public static HookRegistration registerMobSpawnPlacementCheck(KineticWorldEvents.Priority priority, KineticWorldEvents.MobSpawnPlacementHandler handler) {
        initialize();
        return add(MOB_SPAWN_PLACEMENT, priority, handler);
    }

    public static HookRegistration registerMobFinalizeSpawn(KineticWorldEvents.Priority priority, KineticWorldEvents.MobFinalizeSpawnHandler handler) {
        initialize();
        return add(MOB_FINALIZE_SPAWN, priority, handler);
    }

    public static HookRegistration registerBabySpawn(KineticWorldEvents.Priority priority, KineticWorldEvents.BabySpawnHandler handler) {
        initialize();
        return add(BABY_SPAWN, priority, handler);
    }

    private static void onLevelLoad(KineticWorldEvents.Priority priority, LevelEvent.Load event) {
        for (KineticWorldEvents.LevelHandler handler : LEVEL_LOAD.get(priority)) {
            handler.handle(event.getLevel());
        }
    }

    private static void onLevelUnload(KineticWorldEvents.Priority priority, LevelEvent.Unload event) {
        for (KineticWorldEvents.LevelHandler handler : LEVEL_UNLOAD.get(priority)) {
            handler.handle(event.getLevel());
        }
    }

    private static void onEntityJoin(KineticWorldEvents.Priority priority, EntityJoinLevelEvent event) {
        EntityJoinContextImpl context = new EntityJoinContextImpl(event);
        for (KineticWorldEvents.EntityJoinHandler handler : ENTITY_JOIN.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onEntityLeave(KineticWorldEvents.Priority priority, EntityLeaveLevelEvent event) {
        for (KineticWorldEvents.EntityLeaveHandler handler : ENTITY_LEAVE.get(priority)) {
            handler.handle(event.getEntity(), event.getLevel());
        }
    }

    private static void onChunkLoad(KineticWorldEvents.Priority priority, ChunkEvent.Load event) {
        ChunkContextImpl context = new ChunkContextImpl(event.getLevel(), event.getChunk(), event.isNewChunk());
        for (KineticWorldEvents.ChunkHandler handler : CHUNK_LOAD.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onChunkUnload(KineticWorldEvents.Priority priority, ChunkEvent.Unload event) {
        ChunkContextImpl context = new ChunkContextImpl(event.getLevel(), event.getChunk(), false);
        for (KineticWorldEvents.ChunkHandler handler : CHUNK_UNLOAD.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onBlockBreak(KineticWorldEvents.Priority priority, BlockEvent.BreakEvent event) {
        BlockBreakContextImpl context = new BlockBreakContextImpl(event);
        for (KineticWorldEvents.BlockBreakHandler handler : BLOCK_BREAK.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onBlockPlace(KineticWorldEvents.Priority priority, BlockEvent.EntityPlaceEvent event) {
        BlockPlaceContextImpl context = new BlockPlaceContextImpl(event);
        for (KineticWorldEvents.BlockPlaceHandler handler : BLOCK_PLACE.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onItemPickup(KineticWorldEvents.Priority priority, EntityItemPickupEvent event) {
        ItemPickupContextImpl context = new ItemPickupContextImpl(event);
        for (KineticWorldEvents.ItemPickupHandler handler : ITEM_PICKUP.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onMobSpawnPlacementCheck(KineticWorldEvents.Priority priority, MobSpawnEvent.SpawnPlacementCheck event) {
        MobSpawnPlacementContextImpl context = new MobSpawnPlacementContextImpl(event);
        for (KineticWorldEvents.MobSpawnPlacementHandler handler : MOB_SPAWN_PLACEMENT.get(priority)) {
            handler.handle(context);
        }
    }

    private static void onMobFinalizeSpawn(KineticWorldEvents.Priority priority, MobSpawnEvent.FinalizeSpawn event) {
        MobFinalizeSpawnContextImpl context = new MobFinalizeSpawnContextImpl(event);
        for (KineticWorldEvents.MobFinalizeSpawnHandler handler : MOB_FINALIZE_SPAWN.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onBabySpawn(KineticWorldEvents.Priority priority, BabyEntitySpawnEvent event) {
        BabySpawnContextImpl context = new BabySpawnContextImpl(event);
        for (KineticWorldEvents.BabySpawnHandler handler : BABY_SPAWN.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static EventPriority toForge(KineticWorldEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static KineticWorldEvents.SpawnPlacementResult fromForgeResult(Event.Result result) {
        return switch (result) {
            case ALLOW -> KineticWorldEvents.SpawnPlacementResult.ALLOW;
            case DENY -> KineticWorldEvents.SpawnPlacementResult.DENY;
            default -> KineticWorldEvents.SpawnPlacementResult.DEFAULT;
        };
    }

    private static Event.Result toForgeResult(KineticWorldEvents.SpawnPlacementResult result) {
        return switch (result) {
            case ALLOW -> Event.Result.ALLOW;
            case DENY -> Event.Result.DENY;
            case DEFAULT -> Event.Result.DEFAULT;
        };
    }

    private record EntityJoinContextImpl(EntityJoinLevelEvent event) implements KineticWorldEvents.EntityJoinContext {
        @Override
        public Entity entity() {
            return event.getEntity();
        }

        @Override
        public LevelAccessor level() {
            return event.getLevel();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record ChunkContextImpl(LevelAccessor level, ChunkAccess chunk, boolean newChunk) implements KineticWorldEvents.ChunkContext {
    }

    private record BlockBreakContextImpl(BlockEvent.BreakEvent event) implements KineticWorldEvents.BlockBreakContext {
        @Override
        public LevelAccessor level() {
            return event.getLevel();
        }

        @Override
        public BlockPos pos() {
            return event.getPos();
        }

        @Override
        public BlockState state() {
            return event.getState();
        }

        @Override
        public Player player() {
            return event.getPlayer();
        }

        @Override
        public int experienceToDrop() {
            return event.getExpToDrop();
        }

        @Override
        public void experienceToDrop(int experience) {
            event.setExpToDrop(experience);
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record BlockPlaceContextImpl(BlockEvent.EntityPlaceEvent event) implements KineticWorldEvents.BlockPlaceContext {
        @Override
        public LevelAccessor level() {
            return event.getLevel();
        }

        @Override
        public BlockPos pos() {
            return event.getPos();
        }

        @Override
        public BlockState state() {
            return event.getPlacedBlock();
        }

        @Override
        public Entity entity() {
            return event.getEntity();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record ItemPickupContextImpl(EntityItemPickupEvent event) implements KineticWorldEvents.ItemPickupContext {
        @Override
        public Player player() {
            return event.getEntity();
        }

        @Override
        public ItemEntity item() {
            return event.getItem();
        }

        @Override
        public ItemStack stack() {
            return event.getItem().getItem();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private record MobSpawnPlacementContextImpl(MobSpawnEvent.SpawnPlacementCheck event) implements KineticWorldEvents.MobSpawnPlacementContext {
        @Override
        public ServerLevel level() {
            return event.getLevel().getLevel();
        }

        @Override
        public EntityType<?> entityType() {
            return event.getEntityType();
        }

        @Override
        public MobSpawnType spawnType() {
            return event.getSpawnType();
        }

        @Override
        public BlockPos pos() {
            return event.getPos();
        }

        @Override
        public KineticWorldEvents.SpawnPlacementResult result() {
            return fromForgeResult(event.getResult());
        }

        @Override
        public void result(KineticWorldEvents.SpawnPlacementResult result) {
            event.setResult(toForgeResult(result));
        }
    }

    private record MobFinalizeSpawnContextImpl(MobSpawnEvent.FinalizeSpawn event) implements KineticWorldEvents.MobFinalizeSpawnContext {
        @Override
        public LivingEntity entity() {
            return event.getEntity();
        }

        @Override
        public LevelAccessor level() {
            return event.getLevel();
        }

        @Override
        public ServerLevel serverLevel() {
            return event.getLevel().getLevel();
        }

        @Override
        public MobSpawnType spawnType() {
            return event.getSpawnType();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setSpawnCancelled(true);
            event.setCanceled(true);
        }

        @Override
        public void cancelSpawn() {
            event.setSpawnCancelled(true);
        }
    }

    private record BabySpawnContextImpl(BabyEntitySpawnEvent event) implements KineticWorldEvents.BabySpawnContext {
        @Override
        public LivingEntity parentA() {
            return event.getParentA();
        }

        @Override
        public LivingEntity parentB() {
            return event.getParentB();
        }

        @Override
        public LivingEntity child() {
            return event.getChild();
        }

        @Override
        public boolean cancelled() {
            return event.isCanceled();
        }

        @Override
        public void cancel() {
            event.setCanceled(true);
        }
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> levelHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.EntityJoinHandler>> entityJoinHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.EntityLeaveHandler>> entityLeaveHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> chunkHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BlockBreakHandler>> blockBreakHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BlockPlaceHandler>> blockPlaceHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.ItemPickupHandler>> itemPickupHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.MobSpawnPlacementHandler>> mobSpawnPlacementHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.MobFinalizeSpawnHandler>> mobFinalizeSpawnHandlers() {
        return buckets();
    }

    private static EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<KineticWorldEvents.BabySpawnHandler>> babySpawnHandlers() {
        return buckets();
    }

    private static <T> EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticWorldEvents.Priority.class);
        for (KineticWorldEvents.Priority priority : KineticWorldEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static <T> HookRegistration add(EnumMap<KineticWorldEvents.Priority, CopyOnWriteArrayList<T>> listeners,
                                            KineticWorldEvents.Priority priority,
                                            T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return () -> bucket.remove(listener);
    }
}
