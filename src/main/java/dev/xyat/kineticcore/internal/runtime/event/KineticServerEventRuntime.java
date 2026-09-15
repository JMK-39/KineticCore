package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
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
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> TICK_START = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> TICK_END = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_TICK_START = playerHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_TICK_END = playerHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> ABOUT_TO_START = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STARTING = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STARTED = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STOPPING = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> STOPPED = serverHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_LOGIN = playerHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> PLAYER_LOGOUT = playerHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerCloneHandler>> PLAYER_CLONE = cloneHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerRespawnHandler>> PLAYER_RESPAWN = respawnHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerDimensionHandler>> PLAYER_DIMENSION = dimensionHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.DatapackSyncHandler>> DATAPACK_SYNC = datapackSyncHandlers();
    private static final EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ChatHandler>> CHAT = chatHandlers();

    private static boolean initialized;

    private KineticServerEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        for (KineticServerEvents.Priority priority : KineticServerEvents.Priority.values()) {
            EventPriority forgePriority = toForge(priority);
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (TickEvent.ServerTickEvent event) -> onServerTick(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (TickEvent.PlayerTickEvent event) -> onPlayerTick(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerAboutToStartEvent event) -> onServerAboutToStart(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStartingEvent event) -> onServerStarting(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStartedEvent event) -> onServerStarted(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStoppingEvent event) -> onServerStopping(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerStoppedEvent event) -> onServerStopped(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerLoggedInEvent event) -> onPlayerLogin(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerLoggedOutEvent event) -> onPlayerLogout(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.Clone event) -> onPlayerClone(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerRespawnEvent event) -> onPlayerRespawn(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (PlayerEvent.PlayerChangedDimensionEvent event) -> onPlayerChangedDimension(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (OnDatapackSyncEvent event) -> onDatapackSync(priority, event));
            MinecraftForge.EVENT_BUS.addListener(forgePriority, (ServerChatEvent event) -> onChat(priority, event));
        }
    }

    public static HookRegistration registerTick(KineticServerEvents.Priority priority, KineticServerEvents.TickPhase phase, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(phase == KineticServerEvents.TickPhase.START ? TICK_START : TICK_END, priority, listener);
    }

    public static HookRegistration registerPlayerTick(KineticServerEvents.Priority priority, KineticServerEvents.TickPhase phase, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(phase == KineticServerEvents.TickPhase.START ? PLAYER_TICK_START : PLAYER_TICK_END, priority, listener);
    }

    public static HookRegistration registerAboutToStart(KineticServerEvents.Priority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(ABOUT_TO_START, priority, listener);
    }

    public static HookRegistration registerStarting(KineticServerEvents.Priority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STARTING, priority, listener);
    }

    public static HookRegistration registerStarted(KineticServerEvents.Priority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STARTED, priority, listener);
    }

    public static HookRegistration registerStopping(KineticServerEvents.Priority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STOPPING, priority, listener);
    }

    public static HookRegistration registerStopped(KineticServerEvents.Priority priority, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STOPPED, priority, listener);
    }

    public static HookRegistration registerPlayerLogin(KineticServerEvents.Priority priority, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGIN, priority, listener);
    }

    public static HookRegistration registerPlayerLogout(KineticServerEvents.Priority priority, KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGOUT, priority, listener);
    }

    public static HookRegistration registerPlayerClone(KineticServerEvents.Priority priority, KineticServerEvents.PlayerCloneHandler listener) {
        initialize();
        return add(PLAYER_CLONE, priority, listener);
    }

    public static HookRegistration registerPlayerRespawn(KineticServerEvents.Priority priority, KineticServerEvents.PlayerRespawnHandler listener) {
        initialize();
        return add(PLAYER_RESPAWN, priority, listener);
    }

    public static HookRegistration registerPlayerChangedDimension(KineticServerEvents.Priority priority, KineticServerEvents.PlayerDimensionHandler listener) {
        initialize();
        return add(PLAYER_DIMENSION, priority, listener);
    }

    public static HookRegistration registerDatapackSync(KineticServerEvents.Priority priority, KineticServerEvents.DatapackSyncHandler listener) {
        initialize();
        return add(DATAPACK_SYNC, priority, listener);
    }

    public static HookRegistration registerChat(KineticServerEvents.Priority priority, KineticServerEvents.ChatHandler listener) {
        initialize();
        return add(CHAT, priority, listener);
    }

    private static void onServerTick(KineticServerEvents.Priority priority, TickEvent.ServerTickEvent event) {
        CopyOnWriteArrayList<KineticServerEvents.ServerHandler> listeners =
                event.phase == TickEvent.Phase.START ? TICK_START.get(priority) : TICK_END.get(priority);
        for (KineticServerEvents.ServerHandler listener : listeners) {
            listener.handle(event.getServer());
        }
    }

    private static void onPlayerTick(KineticServerEvents.Priority priority, TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        CopyOnWriteArrayList<KineticServerEvents.PlayerHandler> listeners =
                event.phase == TickEvent.Phase.START ? PLAYER_TICK_START.get(priority) : PLAYER_TICK_END.get(priority);
        for (KineticServerEvents.PlayerHandler listener : listeners) {
            listener.handle(player);
        }
    }

    private static void onServerAboutToStart(KineticServerEvents.Priority priority, ServerAboutToStartEvent event) {
        for (KineticServerEvents.ServerHandler listener : ABOUT_TO_START.get(priority)) {
            listener.handle(event.getServer());
        }
    }

    private static void onServerStarting(KineticServerEvents.Priority priority, ServerStartingEvent event) {
        for (KineticServerEvents.ServerHandler listener : STARTING.get(priority)) {
            listener.handle(event.getServer());
        }
    }

    private static void onServerStarted(KineticServerEvents.Priority priority, ServerStartedEvent event) {
        for (KineticServerEvents.ServerHandler listener : STARTED.get(priority)) {
            listener.handle(event.getServer());
        }
    }

    private static void onServerStopping(KineticServerEvents.Priority priority, ServerStoppingEvent event) {
        for (KineticServerEvents.ServerHandler listener : STOPPING.get(priority)) {
            listener.handle(event.getServer());
        }
    }

    private static void onServerStopped(KineticServerEvents.Priority priority, ServerStoppedEvent event) {
        for (KineticServerEvents.ServerHandler listener : STOPPED.get(priority)) {
            listener.handle(event.getServer());
        }
    }

    private static void onPlayerLogin(KineticServerEvents.Priority priority, PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerHandler listener : PLAYER_LOGIN.get(priority)) {
            listener.handle(player);
        }
    }

    private static void onPlayerLogout(KineticServerEvents.Priority priority, PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerHandler listener : PLAYER_LOGOUT.get(priority)) {
            listener.handle(player);
        }
    }

    private static void onPlayerClone(KineticServerEvents.Priority priority, PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original) || !(event.getEntity() instanceof ServerPlayer current)) return;
        for (KineticServerEvents.PlayerCloneHandler listener : PLAYER_CLONE.get(priority)) {
            listener.handle(original, current, event.isWasDeath());
        }
    }

    private static void onPlayerRespawn(KineticServerEvents.Priority priority, PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerRespawnHandler listener : PLAYER_RESPAWN.get(priority)) {
            listener.handle(player, event.isEndConquered());
        }
    }

    private static void onPlayerChangedDimension(KineticServerEvents.Priority priority, PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerDimensionHandler listener : PLAYER_DIMENSION.get(priority)) {
            listener.handle(player, event.getFrom(), event.getTo());
        }
    }

    private static void onDatapackSync(KineticServerEvents.Priority priority, OnDatapackSyncEvent event) {
        for (KineticServerEvents.DatapackSyncHandler listener : DATAPACK_SYNC.get(priority)) {
            listener.handle(event.getPlayerList().getServer(), event.getPlayer());
        }
    }

    private static void onChat(KineticServerEvents.Priority priority, ServerChatEvent event) {
        ChatContextImpl context = new ChatContextImpl(event);
        for (KineticServerEvents.ChatHandler listener : CHAT.get(priority)) {
            listener.handle(context);
            if (context.cancelled()) break;
        }
    }

    private static EventPriority toForge(KineticServerEvents.Priority priority) {
        return switch (priority) {
            case HIGHEST -> EventPriority.HIGHEST;
            case HIGH -> EventPriority.HIGH;
            case NORMAL -> EventPriority.NORMAL;
            case LOW -> EventPriority.LOW;
            case LOWEST -> EventPriority.LOWEST;
        };
    }

    private static <T> HookRegistration add(EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<T>> listeners,
                                            KineticServerEvents.Priority priority,
                                            T listener) {
        CopyOnWriteArrayList<T> bucket = listeners.get(priority);
        bucket.add(listener);
        return () -> bucket.remove(listener);
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ServerHandler>> serverHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerHandler>> playerHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerCloneHandler>> cloneHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerRespawnHandler>> respawnHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.PlayerDimensionHandler>> dimensionHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.DatapackSyncHandler>> datapackSyncHandlers() {
        return buckets();
    }

    private static EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<KineticServerEvents.ChatHandler>> chatHandlers() {
        return buckets();
    }

    private record ChatContextImpl(ServerChatEvent event) implements KineticServerEvents.ChatContext {
        @Override
        public ServerPlayer player() {
            return event.getPlayer();
        }

        @Override
        public net.minecraft.network.chat.Component message() {
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

    private static <T> EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<T>> buckets() {
        EnumMap<KineticServerEvents.Priority, CopyOnWriteArrayList<T>> result = new EnumMap<>(KineticServerEvents.Priority.class);
        for (KineticServerEvents.Priority priority : KineticServerEvents.Priority.values()) {
            result.put(priority, new CopyOnWriteArrayList<>());
        }
        return result;
    }
}
