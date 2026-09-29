package dev.xyat.kineticcore.api.player.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticPlayerEventRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
public final class KineticPlayerEvents {
    /** A player is about to attack an entity with a melee hit. */
    public interface AttackEntityContext {
        /** Returns the attacking player. */
        Player player();

        /** Returns the entity being attacked. */
        Entity target();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the attack does not happen. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for attack entity notifications. */
    @FunctionalInterface
    public interface AttackEntityHandler {
        /**
         * Called on the thread of the logical side that fired the event; check
         * {@code player().level().isClientSide()} when only one side matters. Cancel on both sides to keep client
         * and server in sync.
         */
        void handle(AttackEntityContext context);
    }

    /** A player left-clicks a block, starting or continuing to mine it. */
    public interface LeftClickBlockContext {
        /** Returns the clicking player. */
        Player player();

        /** Returns the stack in the used hand; empty when the hand is empty. */
        ItemStack stack();

        /** Returns the clicked block position. */
        BlockPos pos();

        /** Returns the clicked face, or {@code null} when unknown. */
        Direction face();

        /** Returns the hand used; always the main hand for left clicks. */
        InteractionHand hand();

        /**
         * Returns the interaction result reported to the game when the event is cancelled;
         * {@link InteractionResult#PASS} by default.
         */
        InteractionResult cancellationResult();

        /**
         * Sets the interaction result reported when the event is cancelled, for example
         * {@link InteractionResult#SUCCESS} to swing the arm.
         */
        void cancellationResult(InteractionResult result);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the block is not hit or mined. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for left click block notifications. */
    @FunctionalInterface
    public interface LeftClickBlockHandler {
        /**
         * Called on the thread of the logical side that fired the event; check
         * {@code player().level().isClientSide()} when only one side matters. Cancel on both sides to keep client
         * and server in sync.
         */
        void handle(LeftClickBlockContext context);
    }

    /** Callback contract for start tracking notifications. */
    @FunctionalInterface
    public interface StartTrackingHandler {
        /**
         * Called on the server thread when {@code target} comes into view of {@code player}; a good moment to send
         * the player extra entity data.
         */
        void handle(Player player, Entity target);
    }

    /** A player right-clicks a block, before the block or held item reacts. */
    public interface RightClickBlockContext {
        /** Returns the clicking player. */
        Player player();

        /** Returns the level of the block. */
        Level level();

        /** Returns the stack in the used hand; empty when the hand is empty. */
        ItemStack stack();

        /** Returns the clicked block position. */
        BlockPos pos();

        /** Returns the clicked face, or {@code null} when unknown. */
        Direction face();

        /** Returns the hand used; the event fires once per hand. */
        InteractionHand hand();

        /**
         * Returns the interaction result reported to the game when the event is cancelled;
         * {@link InteractionResult#PASS} by default.
         */
        InteractionResult cancellationResult();

        /**
         * Sets the interaction result reported when the event is cancelled, for example
         * {@link InteractionResult#SUCCESS} to swing the arm.
         */
        void cancellationResult(InteractionResult result);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /**
         * Cancels the event: neither the block nor the item reacts. Later Kinetic handlers of this event are
         * skipped.
         */
        void cancel();
    }

    /** Callback contract for right click block notifications. */
    @FunctionalInterface
    public interface RightClickBlockHandler {
        /**
         * Called on the thread of the logical side that fired the event; check
         * {@code player().level().isClientSide()} when only one side matters. Cancel on both sides to keep client
         * and server in sync.
         */
        void handle(RightClickBlockContext context);
    }

    /** A player right-clicks with an item without targeting a block or entity. */
    public interface RightClickItemContext {
        /** Returns the clicking player. */
        Player player();

        /** Returns the player's level. */
        Level level();

        /** Returns the used stack. */
        ItemStack stack();

        /** Returns the hand used; the event fires once per hand. */
        InteractionHand hand();

        /**
         * Returns the interaction result reported to the game when the event is cancelled;
         * {@link InteractionResult#PASS} by default.
         */
        InteractionResult cancellationResult();

        /**
         * Sets the interaction result reported when the event is cancelled, for example
         * {@link InteractionResult#SUCCESS} to swing the arm.
         */
        void cancellationResult(InteractionResult result);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the item is not used. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for right click item notifications. */
    @FunctionalInterface
    public interface RightClickItemHandler {
        /**
         * Called on the thread of the logical side that fired the event; check
         * {@code player().level().isClientSide()} when only one side matters. Cancel on both sides to keep client
         * and server in sync.
         */
        void handle(RightClickItemContext context);
    }

    /** Callback contract for wake up notifications. */
    @FunctionalInterface
    public interface WakeUpHandler {
        /** Called when the player leaves a bed, on both logical sides. */
        void handle(Player player);
    }

    /** Callback contract for container open notifications. */
    @FunctionalInterface
    public interface ContainerOpenHandler {
        /** Called on the server thread after a container menu was opened for the player. */
        void handle(Player player, AbstractContainerMenu menu);
    }

    /** The game computes how fast a player mines a block. Fired every tick while mining, on both sides. */
    public interface BreakSpeedContext {
        /** Returns the mining player. */
        Player player();

        /** Returns the block being mined. */
        BlockState state();

        /** Returns the block position, or the player's position when the game does not provide one. */
        BlockPos pos();

        /** Returns the mining speed after tool, effect and earlier handler adjustments. */
        float speed();

        /** Replaces the mining speed; higher is faster. Set it on both sides or mining progress desyncs. */
        void speed(float speed);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the block cannot be mined. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for break speed notifications. */
    @FunctionalInterface
    public interface BreakSpeedHandler {
        /** Called on both logical sides every tick while the player mines; keep it cheap. */
        void handle(BreakSpeedContext context);
    }

    /** The game checks whether a player's tool can harvest a block's drops. */
    public interface HarvestCheckContext {
        /** Returns the player. */
        Player player();

        /** Returns the block being harvested. */
        BlockState state();

        /** Returns whether the block will drop its items, including changes by earlier handlers. */
        boolean canHarvest();

        /** Sets whether the block will drop its items. */
        void canHarvest(boolean canHarvest);
    }

    /** Callback contract for harvest check notifications. */
    @FunctionalInterface
    public interface HarvestCheckHandler {
        /** Called on the thread of the side that checks harvesting. */
        void handle(HarvestCheckContext context);
    }

    private KineticPlayerEvents() {
    }

    /**
     * Subscribes to player melee attacks ({@code AttackEntityEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onAttackEntity(KineticEventPriority priority, AttackEntityHandler handler) {
        return KineticPlayerEventRuntime.registerAttackEntity(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to left clicks on blocks ({@code PlayerInteractEvent.LeftClickBlock}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onLeftClickBlock(KineticEventPriority priority, LeftClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerLeftClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to entities coming into a player's view ({@code PlayerEvent.StartTracking}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onStartTracking(KineticEventPriority priority, StartTrackingHandler handler) {
        return KineticPlayerEventRuntime.registerStartTracking(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to right clicks on blocks ({@code PlayerInteractEvent.RightClickBlock}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onRightClickBlock(KineticEventPriority priority, RightClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to right clicks with items ({@code PlayerInteractEvent.RightClickItem}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onRightClickItem(KineticEventPriority priority, RightClickItemHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickItem(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to players leaving beds ({@code PlayerWakeUpEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onWakeUp(KineticEventPriority priority, WakeUpHandler handler) {
        return KineticPlayerEventRuntime.registerWakeUp(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to container menus opening ({@code PlayerContainerEvent.Open}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onContainerOpen(KineticEventPriority priority, ContainerOpenHandler handler) {
        return KineticPlayerEventRuntime.registerContainerOpen(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to mining speed calculation ({@code PlayerEvent.BreakSpeed}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onBreakSpeed(KineticEventPriority priority, BreakSpeedHandler handler) {
        return KineticPlayerEventRuntime.registerBreakSpeed(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Subscribes to harvest checks ({@code PlayerEvent.HarvestCheck}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param handler callback
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onHarvestCheck(KineticEventPriority priority, HarvestCheckHandler handler) {
        return KineticPlayerEventRuntime.registerHarvestCheck(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
