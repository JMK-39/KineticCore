package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticPlayerEventRuntime {
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.AttackEntityHandler>> ATTACK_ENTITY = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.LeftClickBlockHandler>> LEFT_CLICK_BLOCK = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.RightClickBlockHandler>> RIGHT_CLICK_BLOCK = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.RightClickItemHandler>> RIGHT_CLICK_ITEM = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.StartTrackingHandler>> START_TRACKING = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.ContainerOpenHandler>> CONTAINER_OPEN = buckets();
    private static final EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<KineticPlayerEvents.BreakSpeedHandler>> BREAK_SPEED = buckets();
    private static boolean initialized;

    private KineticPlayerEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        for (KineticPlayerEvents.Priority priority : KineticPlayerEvents.Priority.values()) {
            EventPriority forgePriority = toForge(priority);
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (AttackEntityEvent event) -> onAttackEntity(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.LeftClickBlock event) -> onLeftClickBlock(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.RightClickBlock event) -> onRightClickBlock(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerInteractEvent.RightClickItem event) -> onRightClickItem(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.StartTracking event) -> onStartTracking(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerContainerEvent.Open event) -> onContainerOpen(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.BreakSpeed event) -> onBreakSpeed(priority, event));
        }
    }

    public static HookRegistration registerAttackEntity(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.AttackEntityHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.AttackEntityHandler> bucket = ATTACK_ENTITY.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerLeftClickBlock(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.LeftClickBlockHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.LeftClickBlockHandler> bucket = LEFT_CLICK_BLOCK.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerRightClickBlock(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.RightClickBlockHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.RightClickBlockHandler> bucket = RIGHT_CLICK_BLOCK.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerRightClickItem(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.RightClickItemHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.RightClickItemHandler> bucket = RIGHT_CLICK_ITEM.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerStartTracking(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.StartTrackingHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.StartTrackingHandler> bucket = START_TRACKING.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerContainerOpen(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.ContainerOpenHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.ContainerOpenHandler> bucket = CONTAINER_OPEN.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    public static HookRegistration registerBreakSpeed(
            KineticPlayerEvents.Priority priority,
            KineticPlayerEvents.BreakSpeedHandler handler
    ) {
        initialize();
        CopyOnWriteArrayList<KineticPlayerEvents.BreakSpeedHandler> bucket = BREAK_SPEED.get(priority);
        bucket.add(handler);
        return () -> bucket.remove(handler);
    }

    private static void onAttackEntity(KineticPlayerEvents.Priority priority, AttackEntityEvent event) {
        AttackEntityContextImpl context = new AttackEntityContextImpl(event);
        for (KineticPlayerEvents.AttackEntityHandler handler : ATTACK_ENTITY.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onLeftClickBlock(KineticPlayerEvents.Priority priority, PlayerInteractEvent.LeftClickBlock event) {
        LeftClickBlockContextImpl context = new LeftClickBlockContextImpl(event);
        for (KineticPlayerEvents.LeftClickBlockHandler handler : LEFT_CLICK_BLOCK.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onRightClickBlock(KineticPlayerEvents.Priority priority, PlayerInteractEvent.RightClickBlock event) {
        RightClickBlockContextImpl context = new RightClickBlockContextImpl(event);
        for (KineticPlayerEvents.RightClickBlockHandler handler : RIGHT_CLICK_BLOCK.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onRightClickItem(KineticPlayerEvents.Priority priority, PlayerInteractEvent.RightClickItem event) {
        RightClickItemContextImpl context = new RightClickItemContextImpl(event);
        for (KineticPlayerEvents.RightClickItemHandler handler : RIGHT_CLICK_ITEM.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static void onStartTracking(KineticPlayerEvents.Priority priority, PlayerEvent.StartTracking event) {
        for (KineticPlayerEvents.StartTrackingHandler handler : START_TRACKING.get(priority)) {
            handler.handle(event.getEntity(), event.getTarget());
        }
    }

    private static void onContainerOpen(KineticPlayerEvents.Priority priority, PlayerContainerEvent.Open event) {
        for (KineticPlayerEvents.ContainerOpenHandler handler : CONTAINER_OPEN.get(priority)) {
            handler.handle(event.getEntity(), event.getContainer());
        }
    }

    private static void onBreakSpeed(KineticPlayerEvents.Priority priority, PlayerEvent.BreakSpeed event) {
        BreakSpeedContextImpl context = new BreakSpeedContextImpl(event);
        for (KineticPlayerEvents.BreakSpeedHandler handler : BREAK_SPEED.get(priority)) {
            handler.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static EventPriority toForge(KineticPlayerEvents.Priority priority) {
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

    private static <T> EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticPlayerEvents.Priority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticPlayerEvents.Priority.class);
        for (KineticPlayerEvents.Priority priority : KineticPlayerEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
