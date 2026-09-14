package dev.xyat.kineticcore.api.server.event;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.internal.runtime.event.KineticServerEventRuntime;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Objects;

public final class KineticServerEvents {
    public enum TickPhase {
        START,
        END
    }

    @FunctionalInterface
    public interface ServerHandler {
        void handle(MinecraftServer server);
    }

    @FunctionalInterface
    public interface PlayerHandler {
        void handle(ServerPlayer player);
    }

    @FunctionalInterface
    public interface PlayerRespawnHandler {
        void handle(ServerPlayer player, boolean endConquered);
    }

    @FunctionalInterface
    public interface PlayerDimensionHandler {
        void handle(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to);
    }

    private KineticServerEvents() {
    }

    public static HookRegistration onTick(TickPhase phase, ServerHandler listener) {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerTick(phase, listener);
    }

    public static HookRegistration onStarted(ServerHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarted(listener);
    }

    public static HookRegistration onPlayerLogin(PlayerHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogin(listener);
    }

    public static HookRegistration onPlayerLogout(PlayerHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogout(listener);
    }

    public static HookRegistration onPlayerRespawn(PlayerRespawnHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerRespawn(listener);
    }

    public static HookRegistration onPlayerChangedDimension(PlayerDimensionHandler listener) {
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerChangedDimension(listener);
    }
}
