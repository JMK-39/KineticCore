package dev.xyat.kineticcore.api.player.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.runtime.event.KineticPlayerEventRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public final class KineticPlayerEvents {
    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }

    public interface AttackEntityContext {
        Player player();

        Entity target();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface AttackEntityHandler {
        void handle(AttackEntityContext context);
    }

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

    @FunctionalInterface
    public interface LeftClickBlockHandler {
        void handle(LeftClickBlockContext context);
    }

    @FunctionalInterface
    public interface StartTrackingHandler {
        void handle(Player player, Entity target);
    }

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

    @FunctionalInterface
    public interface RightClickBlockHandler {
        void handle(RightClickBlockContext context);
    }

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

    @FunctionalInterface
    public interface RightClickItemHandler {
        void handle(RightClickItemContext context);
    }

    @FunctionalInterface
    public interface ContainerOpenHandler {
        void handle(Player player, AbstractContainerMenu menu);
    }

    public interface BreakSpeedContext {
        Player player();

        BlockState state();

        BlockPos pos();

        float speed();

        void speed(float speed);

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface BreakSpeedHandler {
        void handle(BreakSpeedContext context);
    }

    private KineticPlayerEvents() {
    }

    public static HookRegistration onAttackEntity(AttackEntityHandler handler) {
        return onAttackEntity(Priority.NORMAL, handler);
    }

    public static HookRegistration onAttackEntity(Priority priority, AttackEntityHandler handler) {
        return KineticPlayerEventRuntime.registerAttackEntity(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onLeftClickBlock(LeftClickBlockHandler handler) {
        return onLeftClickBlock(Priority.NORMAL, handler);
    }

    public static HookRegistration onLeftClickBlock(Priority priority, LeftClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerLeftClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onStartTracking(StartTrackingHandler handler) {
        return onStartTracking(Priority.NORMAL, handler);
    }

    public static HookRegistration onStartTracking(Priority priority, StartTrackingHandler handler) {
        return KineticPlayerEventRuntime.registerStartTracking(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onRightClickBlock(RightClickBlockHandler handler) {
        return onRightClickBlock(Priority.NORMAL, handler);
    }

    public static HookRegistration onRightClickBlock(Priority priority, RightClickBlockHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickBlock(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onRightClickItem(RightClickItemHandler handler) {
        return onRightClickItem(Priority.NORMAL, handler);
    }

    public static HookRegistration onRightClickItem(Priority priority, RightClickItemHandler handler) {
        return KineticPlayerEventRuntime.registerRightClickItem(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onContainerOpen(ContainerOpenHandler handler) {
        return onContainerOpen(Priority.NORMAL, handler);
    }

    public static HookRegistration onContainerOpen(Priority priority, ContainerOpenHandler handler) {
        return KineticPlayerEventRuntime.registerContainerOpen(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onBreakSpeed(BreakSpeedHandler handler) {
        return onBreakSpeed(Priority.NORMAL, handler);
    }

    public static HookRegistration onBreakSpeed(Priority priority, BreakSpeedHandler handler) {
        return KineticPlayerEventRuntime.registerBreakSpeed(
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(handler, "handler")
        );
    }
}
