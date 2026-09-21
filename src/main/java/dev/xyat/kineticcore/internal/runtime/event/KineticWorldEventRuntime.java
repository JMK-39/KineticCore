package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
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
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> LEVEL_LOAD = levelHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> LEVEL_UNLOAD = levelHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.EntityJoinHandler>> ENTITY_JOIN = entityJoinHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.EntityLeaveHandler>> ENTITY_LEAVE = entityLeaveHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> CHUNK_LOAD = chunkHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> CHUNK_UNLOAD = chunkHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BlockBreakHandler>> BLOCK_BREAK = blockBreakHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BlockPlaceHandler>> BLOCK_PLACE = blockPlaceHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.FarmlandTrampleHandler>> FARMLAND_TRAMPLE = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.ItemPickupHandler>> ITEM_PICKUP = itemPickupHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.MobSpawnPlacementHandler>> MOB_SPAWN_PLACEMENT = mobSpawnPlacementHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.MobFinalizeSpawnHandler>> MOB_FINALIZE_SPAWN = mobFinalizeSpawnHandlers();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BabySpawnHandler>> BABY_SPAWN = babySpawnHandlers();

    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticWorldEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;

        for (KineticEventPriority priority : KineticEventPriority.values()) {
            EventPriority forgePriority = toForge(priority);
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LevelEvent.Load event) -> onLevelLoad(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (LevelEvent.Unload event) -> onLevelUnload(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityJoinLevelEvent event) -> onEntityJoin(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityLeaveLevelEvent event) -> onEntityLeave(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ChunkEvent.Load event) -> onChunkLoad(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ChunkEvent.Unload event) -> onChunkUnload(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (BlockEvent.BreakEvent event) -> onBlockBreak(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (BlockEvent.EntityPlaceEvent event) -> onBlockPlace(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (BlockEvent.FarmlandTrampleEvent event) -> onFarmlandTrample(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (EntityItemPickupEvent event) -> onItemPickup(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobSpawnEvent.SpawnPlacementCheck event) -> onMobSpawnPlacementCheck(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (MobSpawnEvent.FinalizeSpawn event) -> onMobFinalizeSpawn(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (BabyEntitySpawnEvent event) -> onBabySpawn(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    public static KineticEventSubscription registerLevelLoad(KineticEventPriority priority, KineticWorldEvents.LevelHandler handler) {
        initialize();
        return add(LEVEL_LOAD, priority, handler);
    }

    public static KineticEventSubscription registerLevelUnload(KineticEventPriority priority, KineticWorldEvents.LevelHandler handler) {
        initialize();
        return add(LEVEL_UNLOAD, priority, handler);
    }

    public static KineticEventSubscription registerEntityJoin(KineticEventPriority priority, KineticWorldEvents.EntityJoinHandler handler) {
        initialize();
        return add(ENTITY_JOIN, priority, handler);
    }

    public static KineticEventSubscription registerEntityLeave(KineticEventPriority priority, KineticWorldEvents.EntityLeaveHandler handler) {
        initialize();
        return add(ENTITY_LEAVE, priority, handler);
    }

    public static KineticEventSubscription registerChunkLoad(KineticEventPriority priority, KineticWorldEvents.ChunkHandler handler) {
        initialize();
        return add(CHUNK_LOAD, priority, handler);
    }

    public static KineticEventSubscription registerChunkUnload(KineticEventPriority priority, KineticWorldEvents.ChunkHandler handler) {
        initialize();
        return add(CHUNK_UNLOAD, priority, handler);
    }

    public static KineticEventSubscription registerBlockBreak(KineticEventPriority priority, KineticWorldEvents.BlockBreakHandler handler) {
        initialize();
        return add(BLOCK_BREAK, priority, handler);
    }

    public static KineticEventSubscription registerBlockPlace(KineticEventPriority priority, KineticWorldEvents.BlockPlaceHandler handler) {
        initialize();
        return add(BLOCK_PLACE, priority, handler);
    }

    public static KineticEventSubscription registerFarmlandTrample(KineticEventPriority priority, KineticWorldEvents.FarmlandTrampleHandler handler) {
        initialize();
        return add(FARMLAND_TRAMPLE, priority, handler);
    }

    public static KineticEventSubscription registerItemPickup(KineticEventPriority priority, KineticWorldEvents.ItemPickupHandler handler) {
        initialize();
        return add(ITEM_PICKUP, priority, handler);
    }

    public static KineticEventSubscription registerMobSpawnPlacementCheck(KineticEventPriority priority, KineticWorldEvents.MobSpawnPlacementHandler handler) {
        initialize();
        return add(MOB_SPAWN_PLACEMENT, priority, handler);
    }

    public static KineticEventSubscription registerMobFinalizeSpawn(KineticEventPriority priority, KineticWorldEvents.MobFinalizeSpawnHandler handler) {
        initialize();
        return add(MOB_FINALIZE_SPAWN, priority, handler);
    }

    public static KineticEventSubscription registerBabySpawn(KineticEventPriority priority, KineticWorldEvents.BabySpawnHandler handler) {
        initialize();
        return add(BABY_SPAWN, priority, handler);
    }

    private static void onLevelLoad(KineticEventPriority priority, LevelEvent.Load event) {
        KineticCallbackBatch.runAll(LEVEL_LOAD.get(priority), handler -> handler.handle(event.getLevel()));
    }

    private static void onLevelUnload(KineticEventPriority priority, LevelEvent.Unload event) {
        KineticCallbackBatch.runAll(LEVEL_UNLOAD.get(priority), handler -> handler.handle(event.getLevel()));
    }

    private static void onEntityJoin(KineticEventPriority priority, EntityJoinLevelEvent event) {
        EntityJoinContextImpl context = new EntityJoinContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                ENTITY_JOIN.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onEntityLeave(KineticEventPriority priority, EntityLeaveLevelEvent event) {
        KineticCallbackBatch.runAll(ENTITY_LEAVE.get(priority), handler -> handler.handle(event.getEntity(), event.getLevel()));
    }

    private static void onChunkLoad(KineticEventPriority priority, ChunkEvent.Load event) {
        ChunkContextImpl context = new ChunkContextImpl(event.getLevel(), event.getChunk(), event.isNewChunk());
        KineticCallbackBatch.runAll(CHUNK_LOAD.get(priority), handler -> handler.handle(context));
    }

    private static void onChunkUnload(KineticEventPriority priority, ChunkEvent.Unload event) {
        ChunkContextImpl context = new ChunkContextImpl(event.getLevel(), event.getChunk(), false);
        KineticCallbackBatch.runAll(CHUNK_UNLOAD.get(priority), handler -> handler.handle(context));
    }

    private static void onBlockBreak(KineticEventPriority priority, BlockEvent.BreakEvent event) {
        BlockBreakContextImpl context = new BlockBreakContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                BLOCK_BREAK.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onBlockPlace(KineticEventPriority priority, BlockEvent.EntityPlaceEvent event) {
        BlockPlaceContextImpl context = new BlockPlaceContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                BLOCK_PLACE.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onFarmlandTrample(KineticEventPriority priority, BlockEvent.FarmlandTrampleEvent event) {
        FarmlandTrampleContextImpl context = new FarmlandTrampleContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                FARMLAND_TRAMPLE.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onItemPickup(KineticEventPriority priority, EntityItemPickupEvent event) {
        ItemPickupContextImpl context = new ItemPickupContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                ITEM_PICKUP.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onMobSpawnPlacementCheck(KineticEventPriority priority, MobSpawnEvent.SpawnPlacementCheck event) {
        MobSpawnPlacementContextImpl context = new MobSpawnPlacementContextImpl(event);
        KineticCallbackBatch.runAll(MOB_SPAWN_PLACEMENT.get(priority), handler -> handler.handle(context));
    }

    private static void onMobFinalizeSpawn(KineticEventPriority priority, MobSpawnEvent.FinalizeSpawn event) {
        MobFinalizeSpawnContextImpl context = new MobFinalizeSpawnContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                MOB_FINALIZE_SPAWN.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onBabySpawn(KineticEventPriority priority, BabyEntitySpawnEvent event) {
        BabySpawnContextImpl context = new BabySpawnContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                BABY_SPAWN.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
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

    private record FarmlandTrampleContextImpl(BlockEvent.FarmlandTrampleEvent event) implements KineticWorldEvents.FarmlandTrampleContext {
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
        public Entity entity() {
            return event.getEntity();
        }

        @Override
        public float fallDistance() {
            return event.getFallDistance();
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

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.LevelHandler>> levelHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.EntityJoinHandler>> entityJoinHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.EntityLeaveHandler>> entityLeaveHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.ChunkHandler>> chunkHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BlockBreakHandler>> blockBreakHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BlockPlaceHandler>> blockPlaceHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.ItemPickupHandler>> itemPickupHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.MobSpawnPlacementHandler>> mobSpawnPlacementHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.MobFinalizeSpawnHandler>> mobFinalizeSpawnHandlers() {
        return buckets();
    }

    private static EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticWorldEvents.BabySpawnHandler>> babySpawnHandlers() {
        return buckets();
    }

    private static <T> EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }

    private static <T> KineticEventSubscription add(EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> listeners,
                                            KineticEventPriority priority,
                                            T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return KineticEventSubscription.once(() -> bucket.remove(listener));
    }
}
