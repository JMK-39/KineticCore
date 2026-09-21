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
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException
 * 时仍执行后续回调，结束后抛出首个异常并附加后续异常。
 * 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 */
public final class KineticPlayerEvents {
    /** Context exposed to attack entity callbacks. */
    public interface AttackEntityContext {
        Player player();

        Entity target();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for attack entity notifications. */
    @FunctionalInterface
    public interface AttackEntityHandler {
        void handle(AttackEntityContext context);
    }

    /** Context exposed to left click block callbacks. */
    public interface LeftClickBlockContext {
        Player player();

        ItemStack stack();

        BlockPos pos();

        Direction face();

        InteractionHand hand();

        InteractionResult cancellationResult();

        void cancellationResult(InteractionResult result);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for left click block notifications. */
    @FunctionalInterface
    public interface LeftClickBlockHandler {
        void handle(LeftClickBlockContext context);
    }

    /** Callback contract for start tracking notifications. */
    @FunctionalInterface
    public interface StartTrackingHandler {
        void handle(Player player, Entity target);
    }

    /** Context exposed to right click block callbacks. */
    public interface RightClickBlockContext {
        Player player();

        Level level();

        ItemStack stack();

        BlockPos pos();

        Direction face();

        InteractionHand hand();

        InteractionResult cancellationResult();

        void cancellationResult(InteractionResult result);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for right click block notifications. */
    @FunctionalInterface
    public interface RightClickBlockHandler {
        void handle(RightClickBlockContext context);
    }

    /** Context exposed to right click item callbacks. */
    public interface RightClickItemContext {
        Player player();

        Level level();

        ItemStack stack();

        InteractionHand hand();

        InteractionResult cancellationResult();

        void cancellationResult(InteractionResult result);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for right click item notifications. */
    @FunctionalInterface
    public interface RightClickItemHandler {
        void handle(RightClickItemContext context);
    }

    /** Callback contract for wake up notifications. */
    @FunctionalInterface
    public interface WakeUpHandler {
        void handle(Player player);
    }

    /** Callback contract for container open notifications. */
    @FunctionalInterface
    public interface ContainerOpenHandler {
        void handle(Player player, AbstractContainerMenu menu);
    }

    /** Context exposed to break speed callbacks. */
    public interface BreakSpeedContext {
        Player player();

        BlockState state();

        BlockPos pos();

        float speed();

        void speed(float speed);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for break speed notifications. */
    @FunctionalInterface
    public interface BreakSpeedHandler {
        void handle(BreakSpeedContext context);
    }

    /** Context exposed to harvest check callbacks. */
    public interface HarvestCheckContext {
        Player player();

        BlockState state();

        boolean canHarvest();

        void canHarvest(boolean canHarvest);
    }

    /** Callback contract for harvest check notifications. */
    @FunctionalInterface
    public interface HarvestCheckHandler {
        void handle(HarvestCheckContext context);
    }

    private KineticPlayerEvents() {
    }

    /**
     * Registers a listener for attack entity.
     */
    public static KineticEventSubscription onAttackEntity(KineticEventPriority priority, AttackEntityHandler handler) {
        return KineticPlayerEventRuntime.registerAttackEntity(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for left click block.
     */
    public static KineticEventSubscription onLeftClickBlock(KineticEventPriority priority, LeftClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerLeftClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for start tracking.
     */
    public static KineticEventSubscription onStartTracking(KineticEventPriority priority, StartTrackingHandler handler) {
        return KineticPlayerEventRuntime.registerStartTracking(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for right click block.
     */
    public static KineticEventSubscription onRightClickBlock(KineticEventPriority priority, RightClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for right click item.
     */
    public static KineticEventSubscription onRightClickItem(KineticEventPriority priority, RightClickItemHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickItem(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for wake up.
     */
    public static KineticEventSubscription onWakeUp(KineticEventPriority priority, WakeUpHandler handler) {
        return KineticPlayerEventRuntime.registerWakeUp(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for container open.
     */
    public static KineticEventSubscription onContainerOpen(KineticEventPriority priority, ContainerOpenHandler handler) {
        return KineticPlayerEventRuntime.registerContainerOpen(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for break speed.
     */
    public static KineticEventSubscription onBreakSpeed(KineticEventPriority priority, BreakSpeedHandler handler) {
        return KineticPlayerEventRuntime.registerBreakSpeed(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a listener for harvest check.
     */
    public static KineticEventSubscription onHarvestCheck(KineticEventPriority priority, HarvestCheckHandler handler) {
        return KineticPlayerEventRuntime.registerHarvestCheck(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
