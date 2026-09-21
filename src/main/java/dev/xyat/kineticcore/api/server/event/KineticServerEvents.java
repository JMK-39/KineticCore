package dev.xyat.kineticcore.api.server.event;

import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.runtime.event.KineticServerEventRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException
 * 时仍执行后续回调，结束后抛出首个异常并附加后续异常。
 * 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 */
public final class KineticServerEvents {
    /** Supported tick phase values exposed by this API. */
    public enum TickPhase {
        START,
        END
    }

    /** Callback contract for server notifications. */
    @FunctionalInterface
    public interface ServerHandler {
        void handle(MinecraftServer server);
    }

    /** Callback contract for player notifications. */
    @FunctionalInterface
    public interface PlayerHandler {
        void handle(ServerPlayer player);
    }

    /** Callback contract for player clone notifications. */
    @FunctionalInterface
    public interface PlayerCloneHandler {
        void handle(ServerPlayer original, ServerPlayer current, boolean wasDeath);
    }

    /** Callback contract for player respawn notifications. */
    @FunctionalInterface
    public interface PlayerRespawnHandler {
        void handle(ServerPlayer player, boolean endConquered);
    }

    /** Callback contract for player dimension notifications. */
    @FunctionalInterface
    public interface PlayerDimensionHandler {
        void handle(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to);
    }

    /** Callback contract for datapack sync notifications. */
    @FunctionalInterface
    public interface DatapackSyncHandler {
        void handle(MinecraftServer server, ServerPlayer player);
    }

    /** Context exposed to player game mode change callbacks. */
    public interface PlayerGameModeChangeContext {
        ServerPlayer player();

        GameType currentGameMode();

        GameType newGameMode();

        void setNewGameMode(GameType gameMode);

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for player game mode change notifications. */
    @FunctionalInterface
    public interface PlayerGameModeChangeHandler {
        void handle(PlayerGameModeChangeContext context);
    }

    /** Context exposed to command callbacks. */
    public interface CommandContext {
        CommandSourceStack source();

        String command();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for command notifications. */
    @FunctionalInterface
    public interface CommandHandler {
        void handle(CommandContext context);
    }

    /** Context exposed to chat callbacks. */
    public interface ChatContext {
        ServerPlayer player();

        Component message();

        boolean cancelled();

        void cancel();
    }

    /** Callback contract for chat notifications. */
    @FunctionalInterface
    public interface ChatHandler {
        void handle(ChatContext context);
    }

    private KineticServerEvents() {
    }

    /**
     * Registers a listener for tick.
     */
    public static KineticEventSubscription onTick(KineticEventPriority priority, TickPhase phase, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerTick(priority, phase, listener);
    }

    /**
     * Registers a listener for player tick.
     */
    public static KineticEventSubscription onPlayerTick(KineticEventPriority priority, TickPhase phase, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerTick(priority, phase, listener);
    }

    /**
     * Registers a listener for about to start.
     */
    public static KineticEventSubscription onAboutToStart(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerAboutToStart(priority, listener);
    }

    /**
     * Registers a listener for starting.
     */
    public static KineticEventSubscription onStarting(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarting(priority, listener);
    }

    /**
     * Registers a listener for started.
     */
    public static KineticEventSubscription onStarted(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarted(priority, listener);
    }

    /**
     * Registers a listener for stopping.
     */
    public static KineticEventSubscription onStopping(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopping(priority, listener);
    }

    /**
     * Registers a listener for stopped.
     */
    public static KineticEventSubscription onStopped(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopped(priority, listener);
    }

    /**
     * Registers a listener for player login.
     */
    public static KineticEventSubscription onPlayerLogin(KineticEventPriority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogin(priority, listener);
    }

    /**
     * Registers a listener for player logout.
     */
    public static KineticEventSubscription onPlayerLogout(KineticEventPriority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogout(priority, listener);
    }

    /**
     * Registers a listener for player clone.
     */
    public static KineticEventSubscription onPlayerClone(KineticEventPriority priority, PlayerCloneHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerClone(priority, listener);
    }

    /**
     * Registers a listener for player respawn.
     */
    public static KineticEventSubscription onPlayerRespawn(KineticEventPriority priority, PlayerRespawnHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerRespawn(priority, listener);
    }

    /**
     * Registers a listener for player changed dimension.
     */
    public static KineticEventSubscription onPlayerChangedDimension(KineticEventPriority priority, PlayerDimensionHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerChangedDimension(priority, listener);
    }

    /**
     * Registers a listener for player game mode change.
     */
    public static KineticEventSubscription onPlayerGameModeChange(KineticEventPriority priority, PlayerGameModeChangeHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerGameModeChange(priority, listener);
    }

    /**
     * Registers a listener for command.
     */
    public static KineticEventSubscription onCommand(KineticEventPriority priority, CommandHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerCommand(priority, listener);
    }

    /**
     * Registers a listener for datapack sync.
     */
    public static KineticEventSubscription onDatapackSync(KineticEventPriority priority, DatapackSyncHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerDatapackSync(priority, listener);
    }

    /**
     * Registers a listener for chat.
     */
    public static KineticEventSubscription onChat(KineticEventPriority priority, ChatHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerChat(priority, listener);
    }
}
