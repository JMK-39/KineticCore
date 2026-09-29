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
 * 事件订阅入口。非取消型回调按注册顺序逐一执行；某个回调抛出 RuntimeException 时仍执行后续回调，结束后抛出首个异常并附加后续异常。 取消型回调也逐项处理异常：未取消时继续下一个处理器；一旦取消立即停止，
 * 即使取消方随后抛出异常也不会调用下一个处理器，最后报告首个异常及后续错误。
 *
 * <p>Event subscriptions. Non-cancellable callbacks run one by one in registration order; when one throws a
 * RuntimeException the rest still run, and the first exception is rethrown afterwards with later ones attached as
 * suppressed. Cancellable callbacks isolate exceptions the same way: while the event is not cancelled the next
 * handler runs; once it is cancelled dispatch stops, even if the cancelling handler then throws, and the first
 * exception and later errors are reported at the end.
 */
public final class KineticServerEvents {
    /** Which half of a tick a tick listener runs in. */
    public enum TickPhase {
        /** Before the game processes the tick. */
        START,
        /** After the game processed the tick. */
        END
    }

    /** Callback contract for server notifications. */
    @FunctionalInterface
    public interface ServerHandler {
        /** Called on the server thread with the running server. */
        void handle(MinecraftServer server);
    }

    /** Callback contract for player notifications. */
    @FunctionalInterface
    public interface PlayerHandler {
        /** Called on the server thread with the affected player. */
        void handle(ServerPlayer player);
    }

    /** Callback contract for player clone notifications. */
    @FunctionalInterface
    public interface PlayerCloneHandler {
        /**
         * Called when a new player object replaces the old one, on respawn or when returning from the End. Copy
         * custom data from {@code original} to {@code current} here.
         *
         * @param original the old player object, already removed from the world
         * @param current the new player object
         * @param wasDeath {@code true} after a death, {@code false} when returning from the End
         */
        void handle(ServerPlayer original, ServerPlayer current, boolean wasDeath);
    }

    /** Callback contract for player respawn notifications. */
    @FunctionalInterface
    public interface PlayerRespawnHandler {
        /**
         * Called after the player respawned.
         *
         * @param player the new player object
         * @param endConquered {@code true} when the player came back from the End instead of dying
         */
        void handle(ServerPlayer player, boolean endConquered);
    }

    /** Callback contract for player dimension notifications. */
    @FunctionalInterface
    public interface PlayerDimensionHandler {
        /** Called after the player moved from {@code from} to {@code to}. */
        void handle(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to);
    }

    /** Callback contract for datapack sync notifications. */
    @FunctionalInterface
    public interface DatapackSyncHandler {
        /**
         * Called when data-pack contents are sent to clients: for one player on login, or for everyone after
         * {@code /reload}. Send your own synced data here.
         *
         * @param server the running server
         * @param player the player being synced, or {@code null} when all players are synced
         */
        void handle(MinecraftServer server, ServerPlayer player);
    }

    /** A player's game mode is about to change. */
    public interface PlayerGameModeChangeContext {
        /** Returns the player. */
        ServerPlayer player();

        /** Returns the current game mode. */
        GameType currentGameMode();

        /** Returns the game mode that will be applied. */
        GameType newGameMode();

        /** Replaces the game mode that will be applied. */
        void setNewGameMode(GameType gameMode);

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the game mode does not change. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for player game mode change notifications. */
    @FunctionalInterface
    public interface PlayerGameModeChangeHandler {
        /** Called on the server thread. */
        void handle(PlayerGameModeChangeContext context);
    }

    /** A command was parsed and is about to run. */
    public interface CommandContext {
        /** Returns who runs the command: a player, the console, a command block or a function. */
        CommandSourceStack source();

        /** Returns the full command text without the leading slash. */
        String command();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the command does not run. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for command notifications. */
    @FunctionalInterface
    public interface CommandHandler {
        /** Called on the server thread before the command executes. */
        void handle(CommandContext context);
    }

    /** A player sent a chat message that is about to be broadcast. */
    public interface ChatContext {
        /** Returns the sender. */
        ServerPlayer player();

        /** Returns the message as it will be broadcast. */
        Component message();

        /** Returns whether a handler has already cancelled the event. */
        boolean cancelled();

        /** Cancels the event: the message is not broadcast. Later Kinetic handlers of this event are skipped. */
        void cancel();
    }

    /** Callback contract for chat notifications. */
    @FunctionalInterface
    public interface ChatHandler {
        /** Called on the server thread. */
        void handle(ChatContext context);
    }

    private KineticServerEvents() {
    }

    /**
     * Subscribes to server ticks ({@code TickEvent.ServerTickEvent}). Runs 20 times per second, so keep it cheap.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param phase whether to run before or after the game's own tick logic
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onTick(KineticEventPriority priority, TickPhase phase, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerTick(priority, phase, listener);
    }

    /**
     * Subscribes to server-side player ticks ({@code TickEvent.PlayerTickEvent}). Runs for every player 20 times
     * per second, so keep it cheap.
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param phase whether to run before or after the game's own tick logic
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerTick(KineticEventPriority priority, TickPhase phase, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerTick(priority, phase, listener);
    }

    /**
     * Subscribes to server startup, before worlds load ({@code ServerAboutToStartEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onAboutToStart(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerAboutToStart(priority, listener);
    }

    /**
     * Subscribes to server startup, after worlds load and before players join ({@code ServerStartingEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onStarting(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarting(priority, listener);
    }

    /**
     * Subscribes to the server becoming ready for players ({@code ServerStartedEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onStarted(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarted(priority, listener);
    }

    /**
     * Subscribes to server shutdown, while worlds are still loaded ({@code ServerStoppingEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onStopping(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopping(priority, listener);
    }

    /**
     * Subscribes to the end of server shutdown ({@code ServerStoppedEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onStopped(KineticEventPriority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopped(priority, listener);
    }

    /**
     * Subscribes to players joining ({@code PlayerEvent.PlayerLoggedInEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerLogin(KineticEventPriority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogin(priority, listener);
    }

    /**
     * Subscribes to players leaving ({@code PlayerEvent.PlayerLoggedOutEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerLogout(KineticEventPriority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogout(priority, listener);
    }

    /**
     * Subscribes to player object replacement on respawn ({@code PlayerEvent.Clone}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerClone(KineticEventPriority priority, PlayerCloneHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerClone(priority, listener);
    }

    /**
     * Subscribes to player respawns ({@code PlayerEvent.PlayerRespawnEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerRespawn(KineticEventPriority priority, PlayerRespawnHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerRespawn(priority, listener);
    }

    /**
     * Subscribes to players changing dimension ({@code PlayerEvent.PlayerChangedDimensionEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerChangedDimension(KineticEventPriority priority, PlayerDimensionHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerChangedDimension(priority, listener);
    }

    /**
     * Subscribes to game mode changes ({@code PlayerEvent.PlayerChangeGameModeEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onPlayerGameModeChange(KineticEventPriority priority, PlayerGameModeChangeHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerGameModeChange(priority, listener);
    }

    /**
     * Subscribes to command execution ({@code CommandEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onCommand(KineticEventPriority priority, CommandHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerCommand(priority, listener);
    }

    /**
     * Subscribes to data-pack sync to clients ({@code OnDatapackSyncEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onDatapackSync(KineticEventPriority priority, DatapackSyncHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerDatapackSync(priority, listener);
    }

    /**
     * Subscribes to player chat messages ({@code ServerChatEvent}).
     *
     * @param priority order relative to other Kinetic handlers of the same event
     * @param listener callback, run on the server thread
     * @return a subscription; close it to unsubscribe
     * @throws NullPointerException if an argument is {@code null}
     */
    public static KineticEventSubscription onChat(KineticEventPriority priority, ChatHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerChat(priority, listener);
    }
}
