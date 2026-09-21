package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.internal.runtime.KineticCallbackBatch;

import dev.xyat.kineticcore.internal.runtime.KineticForgeListenerRegistrations;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.EnumMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticServerEventRuntime {
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> TICK_START = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> TICK_END = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_TICK_START = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_TICK_END = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> ABOUT_TO_START = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STARTING = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STARTED = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STOPPING = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STOPPED = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_LOGIN = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_LOGOUT = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerCloneHandler>> PLAYER_CLONE = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerRespawnHandler>> PLAYER_RESPAWN = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerDimensionHandler>> PLAYER_DIMENSION = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.PlayerGameModeChangeHandler>> PLAYER_GAME_MODE = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.CommandHandler>> COMMAND = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.DatapackSyncHandler>> DATAPACK_SYNC = buckets();
    private static final EnumMap<KineticEventPriority, CopyOnWriteArrayList<KineticServerEvents.ChatHandler>> CHAT = buckets();

    private static final KineticForgeListenerRegistrations LISTENER_REGISTRATIONS = new KineticForgeListenerRegistrations();
    private static boolean initialized;

    private KineticServerEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        var attempt = LISTENER_REGISTRATIONS.begin();
        int slot = 0;

        for (KineticEventPriority priority : KineticEventPriority.values()) {
            EventPriority forgePriority = toForge(priority);
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (TickEvent.ServerTickEvent event) -> onServerTick(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (TickEvent.PlayerTickEvent event) -> onPlayerTick(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerAboutToStartEvent event) -> onServerAboutToStart(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStartingEvent event) -> onServerStarting(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStartedEvent event) -> onServerStarted(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStoppingEvent event) -> onServerStopping(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStoppedEvent event) -> onServerStopped(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerLoggedInEvent event) -> onPlayerLogin(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerLoggedOutEvent event) -> onPlayerLogout(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.Clone event) -> onPlayerClone(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerRespawnEvent event) -> onPlayerRespawn(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerChangedDimensionEvent event) -> onPlayerChangedDimension(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerChangeGameModeEvent event) -> onPlayerGameModeChange(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (CommandEvent event) -> onCommand(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (OnDatapackSyncEvent event) -> onDatapackSync(priority, event)));
            attempt.install(slot++, () -> MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerChatEvent event) -> onChat(priority, event)));
        }
        attempt.finish();
        initialized = true;
    }

    public static KineticEventSubscription registerTick(KineticEventPriority priority, KineticServerEvents.TickPhase phase, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(phase == KineticServerEvents.TickPhase.START ? TICK_START : TICK_END, priority, listener);
    }

    public static KineticEventSubscription registerPlayerTick(KineticEventPriority priority, KineticServerEvents.TickPhase phase, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(phase == KineticServerEvents.TickPhase.START ? PLAYER_TICK_START : PLAYER_TICK_END, priority, listener);
    }

    public static KineticEventSubscription registerAboutToStart(KineticEventPriority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(ABOUT_TO_START, priority, listener);
    }

    public static KineticEventSubscription registerStarting(KineticEventPriority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STARTING, priority, listener);
    }

    public static KineticEventSubscription registerStarted(KineticEventPriority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STARTED, priority, listener);
    }

    public static KineticEventSubscription registerStopping(KineticEventPriority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STOPPING, priority, listener);
    }

    public static KineticEventSubscription registerStopped(KineticEventPriority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STOPPED, priority, listener);
    }

    public static KineticEventSubscription registerPlayerLogin(KineticEventPriority priority, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGIN, priority, listener);
    }

    public static KineticEventSubscription registerPlayerLogout(KineticEventPriority priority, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGOUT, priority, listener);
    }

    public static KineticEventSubscription registerPlayerClone(KineticEventPriority priority, KineticServerEvents.PlayerCloneHandler listener) {
        initialize();
        return add(PLAYER_CLONE, priority, listener);
    }

    public static KineticEventSubscription registerPlayerRespawn(KineticEventPriority priority, KineticServerEvents.PlayerRespawnHandler listener) {
        initialize();
        return add(PLAYER_RESPAWN, priority, listener);
    }

    public static KineticEventSubscription registerPlayerChangedDimension(KineticEventPriority priority, KineticServerEvents.PlayerDimensionHandler listener) {
        initialize();
        return add(PLAYER_DIMENSION, priority, listener);
    }

    public static KineticEventSubscription registerPlayerGameModeChange(KineticEventPriority priority, KineticServerEvents.PlayerGameModeChangeHandler listener) {
        initialize();
        return add(PLAYER_GAME_MODE, priority, listener);
    }

    public static KineticEventSubscription registerCommand(KineticEventPriority priority, KineticServerEvents.CommandHandler listener) {
        initialize();
        return add(COMMAND, priority, listener);
    }

    public static KineticEventSubscription registerDatapackSync(KineticEventPriority priority, KineticServerEvents.DatapackSyncHandler listener) {
        initialize();
        return add(DATAPACK_SYNC, priority, listener);
    }

    public static KineticEventSubscription registerChat(KineticEventPriority priority, KineticServerEvents.ChatHandler listener) {
        initialize();
        return add(CHAT, priority, listener);
    }

    private static void onServerTick(KineticEventPriority priority, TickEvent.ServerTickEvent event) {
        CopyOnWriteArrayList<KineticServerEvents.ServerHandler> listeners =
                event.phase == TickEvent.Phase.START ? TICK_START.get(priority) : TICK_END.get(priority);
        KineticCallbackBatch.runAll(listeners, listener -> listener.handle(event.getServer()));
    }

    private static void onPlayerTick(KineticEventPriority priority, TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        CopyOnWriteArrayList<KineticServerEvents.PlayerHandler> listeners =
                event.phase == TickEvent.Phase.START ? PLAYER_TICK_START.get(priority) : PLAYER_TICK_END.get(priority);
        KineticCallbackBatch.runAll(listeners, listener -> listener.handle(player));
    }

    private static void onServerAboutToStart(KineticEventPriority priority, ServerAboutToStartEvent event) {
        KineticCallbackBatch.runAll(ABOUT_TO_START.get(priority), listener -> listener.handle(event.getServer()));
    }

    private static void onServerStarting(KineticEventPriority priority, ServerStartingEvent event) {
        KineticCallbackBatch.runAll(STARTING.get(priority), listener -> listener.handle(event.getServer()));
    }

    private static void onServerStarted(KineticEventPriority priority, ServerStartedEvent event) {
        KineticCallbackBatch.runAll(STARTED.get(priority), listener -> listener.handle(event.getServer()));
    }

    private static void onServerStopping(KineticEventPriority priority, ServerStoppingEvent event) {
        KineticCallbackBatch.runAll(STOPPING.get(priority), listener -> listener.handle(event.getServer()));
    }

    private static void onServerStopped(KineticEventPriority priority, ServerStoppedEvent event) {
        KineticCallbackBatch.runAll(STOPPED.get(priority), listener -> listener.handle(event.getServer()));
    }

    private static void onPlayerLogin(KineticEventPriority priority, PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KineticCallbackBatch.runAll(PLAYER_LOGIN.get(priority), listener -> listener.handle(player));
    }

    private static void onPlayerLogout(KineticEventPriority priority, PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KineticCallbackBatch.runAll(PLAYER_LOGOUT.get(priority), listener -> listener.handle(player));
    }

    private static void onPlayerClone(KineticEventPriority priority, PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original) || !(event.getEntity() instanceof ServerPlayer current)) return;
        KineticCallbackBatch.runAll(PLAYER_CLONE.get(priority), listener -> listener.handle(original, current, event.isWasDeath()));
    }

    private static void onPlayerRespawn(KineticEventPriority priority, PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KineticCallbackBatch.runAll(PLAYER_RESPAWN.get(priority), listener -> listener.handle(player, event.isEndConquered()));
    }

    private static void onPlayerChangedDimension(KineticEventPriority priority, PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        KineticCallbackBatch.runAll(PLAYER_DIMENSION.get(priority), listener -> listener.handle(player, event.getFrom(), event.getTo()));
    }

    private static void onPlayerGameModeChange(KineticEventPriority priority, PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerGameModeChangeContextImpl context = new PlayerGameModeChangeContextImpl(event, player);
        KineticCallbackBatch.runUntilCancelled(
                PLAYER_GAME_MODE.get(priority),
                listener -> listener.handle(context),
                context::cancelled
        );
    }

    private static void onCommand(KineticEventPriority priority, CommandEvent event) {
        CommandContextImpl context = new CommandContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                COMMAND.get(priority),
                listener -> listener.handle(context),
                context::cancelled
        );
    }

    private static void onDatapackSync(KineticEventPriority priority, OnDatapackSyncEvent event) {
        KineticCallbackBatch.runAll(DATAPACK_SYNC.get(priority), listener -> listener.handle(event.getPlayerList().getServer(), event.getPlayer()));
    }

    private static void onChat(KineticEventPriority priority, ServerChatEvent event) {
        ChatContextImpl context = new ChatContextImpl(event);
        KineticCallbackBatch.runUntilCancelled(
                CHAT.get(priority),
                listener -> listener.handle(context),
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

    private static <T> KineticEventSubscription add(EnumMap<KineticEventPriority, CopyOnWriteArrayList<T>> listeners,
                                                     KineticEventPriority priority,
                                                     T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return KineticEventSubscription.once(() -> bucket.remove(listener));
    }

    private record PlayerGameModeChangeContextImpl(PlayerEvent.PlayerChangeGameModeEvent event,
                                                    ServerPlayer player) implements KineticServerEvents.PlayerGameModeChangeContext {
        @Override
        public GameType currentGameMode() {
            return event.getCurrentGameMode();
        }

        @Override
        public GameType newGameMode() {
            return event.getNewGameMode();
        }

        @Override
        public void setNewGameMode(GameType gameMode) {
            event.setNewGameMode(gameMode);
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

    private record CommandContextImpl(CommandEvent event) implements KineticServerEvents.CommandContext {
        @Override
        public CommandSourceStack source() {
            return event.getParseResults().getContext().getSource();
        }

        @Override
        public String command() {
            return event.getParseResults().getReader().getString();
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

    private record ChatContextImpl(ServerChatEvent event) implements KineticServerEvents.ChatContext {
        @Override
        public ServerPlayer player() {
            return event.getPlayer();
        }

        @Override
        public Component message() {
            return event.getMessage();
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
