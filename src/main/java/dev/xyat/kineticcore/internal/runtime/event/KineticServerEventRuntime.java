package dev.xyat.kineticcore.internal.runtime.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticServerEventRuntime {
    private static final CopyOnWriteArrayList<KineticServerEvents.ServerHandler> TICK_START = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.ServerHandler> TICK_END = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.ServerHandler> STARTED = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.PlayerHandler> PLAYER_LOGIN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.PlayerHandler> PLAYER_LOGOUT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.PlayerRespawnHandler> PLAYER_RESPAWN = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<KineticServerEvents.PlayerDimensionHandler> PLAYER_DIMENSION = new CopyOnWriteArrayList<>();

    private static boolean initialized;

    private KineticServerEventRuntime() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onPlayerLogout);
        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(KineticServerEventRuntime::onPlayerChangedDimension);
    }

    public static HookRegistration registerTick(KineticServerEvents.TickPhase phase, KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(phase == KineticServerEvents.TickPhase.START ? TICK_START : TICK_END, listener);
    }

    public static HookRegistration registerStarted(KineticServerEvents.ServerHandler listener) {
        initialize();
        return add(STARTED, listener);
    }

    public static HookRegistration registerPlayerLogin(KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGIN, listener);
    }

    public static HookRegistration registerPlayerLogout(KineticServerEvents.PlayerHandler listener) {
        initialize();
        return add(PLAYER_LOGOUT, listener);
    }

    public static HookRegistration registerPlayerRespawn(KineticServerEvents.PlayerRespawnHandler listener) {
        initialize();
        return add(PLAYER_RESPAWN, listener);
    }

    public static HookRegistration registerPlayerChangedDimension(KineticServerEvents.PlayerDimensionHandler listener) {
        initialize();
        return add(PLAYER_DIMENSION, listener);
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        CopyOnWriteArrayList<KineticServerEvents.ServerHandler> listeners =
                event.phase == TickEvent.Phase.START ? TICK_START : TICK_END;
        for (KineticServerEvents.ServerHandler listener : listeners) {
            listener.handle(event.getServer());
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        for (KineticServerEvents.ServerHandler listener : STARTED) {
            listener.handle(event.getServer());
        }
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerHandler listener : PLAYER_LOGIN) {
            listener.handle(player);
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerHandler listener : PLAYER_LOGOUT) {
            listener.handle(player);
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerRespawnHandler listener : PLAYER_RESPAWN) {
            listener.handle(player, event.isEndConquered());
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (KineticServerEvents.PlayerDimensionHandler listener : PLAYER_DIMENSION) {
            listener.handle(player, event.getFrom(), event.getTo());
        }
    }

    private static <T> HookRegistration add(CopyOnWriteArrayList<T> listeners, T listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
