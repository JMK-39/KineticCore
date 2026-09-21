package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticPlayerEventRuntime {
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.AttackEntityHandler>> ATTACK_ENTITY = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.LeftClickBlockHandler>> LEFT_CLICK_BLOCK = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.RightClickBlockHandler>> RIGHT_CLICK_BLOCK = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.RightClickItemHandler>> RIGHT_CLICK_ITEM = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.WakeUpHandler>> WAKE_UP = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.StartTrackingHandler>> START_TRACKING = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.ContainerOpenHandler>> CONTAINER_OPEN = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.BreakSpeedHandler>> BREAK_SPEED = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticPlayerEvents.HarvestCheckHandler>> HARVEST_CHECK = buckets();
    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticPlayerEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            EventPriority forgePriority = toForge(priority);
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (AttackEntityEvent event) -> onAttackEntity(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.LeftClickBlock event) -> onLeftClickBlock(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.RightClickBlock event) -> onRightClickBlock(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.RightClickItem event) -> onRightClickItem(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerWakeUpEvent event) -> onWakeUp(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.StartTracking event) -> onStartTracking(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerContainerEvent.Open event) -> onContainerOpen(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.BreakSpeed event) -> onBreakSpeed(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.HarvestCheck event) -> onHarvestCheck(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    public static KineticEventSubscription registerAttackEntity(
            KineticEventPriority priority,
            KineticPlayerEvents.AttackEntityHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.AttackEntityHandler> bucket = ATTACK_ENTITY.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerLeftClickBlock(
            KineticEventPriority priority,
            KineticPlayerEvents.LeftClickBlockHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.LeftClickBlockHandler> bucket = LEFT_CLICK_BLOCK.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerRightClickBlock(
            KineticEventPriority priority,
            KineticPlayerEvents.RightClickBlockHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.RightClickBlockHandler> bucket = RIGHT_CLICK_BLOCK.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerRightClickItem(
            KineticEventPriority priority,
            KineticPlayerEvents.RightClickItemHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.RightClickItemHandler> bucket = RIGHT_CLICK_ITEM.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerWakeUp(
            KineticEventPriority priority,
            KineticPlayerEvents.WakeUpHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.WakeUpHandler> bucket = WAKE_UP.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerStartTracking(
            KineticEventPriority priority,
            KineticPlayerEvents.StartTrackingHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.StartTrackingHandler> bucket = START_TRACKING.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerContainerOpen(
            KineticEventPriority priority,
            KineticPlayerEvents.ContainerOpenHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.ContainerOpenHandler> bucket = CONTAINER_OPEN.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerBreakSpeed(
            KineticEventPriority priority,
            KineticPlayerEvents.BreakSpeedHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.BreakSpeedHandler> bucket = BREAK_SPEED.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    public static KineticEventSubscription registerHarvestCheck(
            KineticEventPriority priority,
            KineticPlayerEvents.HarvestCheckHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.HarvestCheckHandler> bucket = HARVEST_CHECK.get(priority);
        bucket.add(handler);
        return KineticEventSubscription.once(() -> bucket.remove(handler));
    }

    private static void onAttackEntity(KineticEventPriority priority, AttackEntityEvent event) {
        AttackEntityContextImpl context = new AttackEntityContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                ATTACK_ENTITY.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onLeftClickBlock(KineticEventPriority priority, PlayerInteractEvent.LeftClickBlock event) {
        LeftClickBlockContextImpl context = new LeftClickBlockContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                LEFT_CLICK_BLOCK.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onRightClickBlock(KineticEventPriority priority, PlayerInteractEvent.RightClickBlock event) {
        RightClickBlockContextImpl context = new RightClickBlockContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                RIGHT_CLICK_BLOCK.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onRightClickItem(KineticEventPriority priority, PlayerInteractEvent.RightClickItem event) {
        RightClickItemContextImpl context = new RightClickItemContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                RIGHT_CLICK_ITEM.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onWakeUp(KineticEventPriority priority, PlayerWakeUpEvent event) {
        KineticCallbackBatch.runAll(WAKE_UP.get(priority), handler -> handler.handle(event.getEntity()));
    }

    private static void onStartTracking(KineticEventPriority priority, PlayerEvent.StartTracking event) {
        KineticCallbackBatch.runAll(START_TRACKING.get(priority), handler -> handler.handle(event.getEntity(), event.getTarget()));
    }

    private static void onContainerOpen(KineticEventPriority priority, PlayerContainerEvent.Open event) {
        KineticCallbackBatch.runAll(CONTAINER_OPEN.get(priority), handler -> handler.handle(event.getEntity(), event.getContainer()));
    }

    private static void onBreakSpeed(KineticEventPriority priority, PlayerEvent.BreakSpeed event) {
        BreakSpeedContextImpl context = new BreakSpeedContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                BREAK_SPEED.get(priority),
                handler -> handler.handle(context),
                context::cancelled
        );
    }

    private static void onHarvestCheck(KineticEventPriority priority, PlayerEvent.HarvestCheck event) {
        HarvestCheckContextImpl context = new HarvestCheckContextImpl(event);
        KineticCallbackBatch.runAll(HARVEST_CHECK.get(priority), handler -> handler.handle(context));
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

    private record AttackEntityContextImpl(AttackEntityEvent event) implements KineticPlayerEvents.AttackEntityContext {
        @Override
        public Player player() {
            return event.getEntity();
        }

        @Override
        public Entity target() {
            return event.getTarget();
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

    private record LeftClickBlockContextImpl(PlayerInteractEvent.LeftClickBlock event) implements KineticPlayerEvents.LeftClickBlockContext {
        @Override
        public Player player() {
            return event.getEntity();
        }

        @Override
        public ItemStack stack() {
            return event.getItemStack();
        }

        @Override
        public BlockPos pos() {
            return event.getPos();
        }

        @Override
        public Direction face() {
            return event.getFace();
        }

        @Override
        public InteractionHand hand() {
            return event.getHand();
        }

        @Override
        public InteractionResult cancellationResult() {
            return event.getCancellationResult();
        }

        @Override
        public void cancellationResult(InteractionResult result) {
            event.setCancellationResult(result);
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

    private record RightClickBlockContextImpl(PlayerInteractEvent.RightClickBlock event) implements KineticPlayerEvents.RightClickBlockContext {
        @Override
        public Player player() { return event.getEntity(); }

        @Override
        public Level level() { return event.getLevel(); }

        @Override
        public ItemStack stack() { return event.getItemStack(); }

        @Override
        public BlockPos pos() { return event.getPos(); }

        @Override
        public Direction face() { return event.getFace(); }

        @Override
        public InteractionHand hand() { return event.getHand(); }

        @Override
        public InteractionResult cancellationResult() { return event.getCancellationResult(); }

        @Override
        public void cancellationResult(InteractionResult result) { event.setCancellationResult(result); }

        @Override
        public boolean cancelled() { return event.isCanceled(); }

        @Override
        public void cancel() { event.setCanceled(true); }
    }

    private record RightClickItemContextImpl(PlayerInteractEvent.RightClickItem event) implements KineticPlayerEvents.RightClickItemContext {
        @Override
        public Player player() { return event.getEntity(); }

        @Override
        public Level level() { return event.getLevel(); }

        @Override
        public ItemStack stack() { return event.getItemStack(); }

        @Override
        public InteractionHand hand() { return event.getHand(); }

        @Override
        public InteractionResult cancellationResult() { return event.getCancellationResult(); }

        @Override
        public void cancellationResult(InteractionResult result) { event.setCancellationResult(result); }

        @Override
        public boolean cancelled() { return event.isCanceled(); }

        @Override
        public void cancel() { event.setCanceled(true); }
    }

    private record HarvestCheckContextImpl(PlayerEvent.HarvestCheck event) implements KineticPlayerEvents.HarvestCheckContext {
        @Override
        public Player player() { return event.getEntity(); }

        @Override
        public BlockState state() { return event.getTargetBlock(); }

        @Override
        public boolean canHarvest() { return event.canHarvest(); }

        @Override
        public void canHarvest(boolean canHarvest) { event.setCanHarvest(canHarvest); }
    }

    private record BreakSpeedContextImpl(PlayerEvent.BreakSpeed event) implements KineticPlayerEvents.BreakSpeedContext {
        @Override
        public Player player() {
            return event.getEntity();
        }

        @Override
        public BlockState state() {
            return event.getState();
        }

        @Override
        public BlockPos pos() {
            return event.getPosition().orElse(event.getEntity().blockPosition());
        }

        @Override
        public float speed() {
            return event.getNewSpeed();
        }

        @Override
        public void speed(float speed) {
            event.setNewSpeed(speed);
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

    private static <T> EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticEventPriority.class);
        for (KineticEventPriority priority : KineticEventPriority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
