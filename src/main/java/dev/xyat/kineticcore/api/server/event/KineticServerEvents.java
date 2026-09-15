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

    public enum Priority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
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
    public interface PlayerCloneHandler {
        void handle(ServerPlayer original, ServerPlayer current, boolean wasDeath);
    }

    @FunctionalInterface
    public interface PlayerRespawnHandler {
        void handle(ServerPlayer player, boolean endConquered);
    }

    @FunctionalInterface
    public interface PlayerDimensionHandler {
        void handle(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to);
    }

    @FunctionalInterface
    public interface DatapackSyncHandler {
        void handle(MinecraftServer server, ServerPlayer player);
    }

    public interface ChatContext {
        ServerPlayer player();

        net.minecraft.network.chat.Component message();

        boolean cancelled();

        void cancel();
    }

    @FunctionalInterface
    public interface ChatHandler {
        void handle(ChatContext context);
    }

    private KineticServerEvents() {
    }

    public static HookRegistration onTick(TickPhase phase, ServerHandler listener) {
        return onTick(Priority.NORMAL, phase, listener);
    }

    public static HookRegistration onTick(Priority priority, TickPhase phase, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerTick(priority, phase, listener);
    }

    public static HookRegistration onPlayerTick(TickPhase phase, PlayerHandler listener) {
        return onPlayerTick(Priority.NORMAL, phase, listener);
    }

    public static HookRegistration onPlayerTick(Priority priority, TickPhase phase, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerTick(priority, phase, listener);
    }

    public static HookRegistration onAboutToStart(ServerHandler listener) {
        return onAboutToStart(Priority.NORMAL, listener);
    }

    public static HookRegistration onAboutToStart(Priority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerAboutToStart(priority, listener);
    }

    public static HookRegistration onStarting(ServerHandler listener) {
        return onStarting(Priority.NORMAL, listener);
    }

    public static HookRegistration onStarting(Priority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarting(priority, listener);
    }

    public static HookRegistration onStarted(ServerHandler listener) {
        return onStarted(Priority.NORMAL, listener);
    }

    public static HookRegistration onStarted(Priority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStarted(priority, listener);
    }

    public static HookRegistration onStopping(ServerHandler listener) {
        return onStopping(Priority.NORMAL, listener);
    }

    public static HookRegistration onStopping(Priority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopping(priority, listener);
    }

    public static HookRegistration onStopped(ServerHandler listener) {
        return onStopped(Priority.NORMAL, listener);
    }

    public static HookRegistration onStopped(Priority priority, ServerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerStopped(priority, listener);
    }

    public static HookRegistration onPlayerLogin(PlayerHandler listener) {
        return onPlayerLogin(Priority.NORMAL, listener);
    }

    public static HookRegistration onPlayerLogin(Priority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogin(priority, listener);
    }

    public static HookRegistration onPlayerLogout(PlayerHandler listener) {
        return onPlayerLogout(Priority.NORMAL, listener);
    }

    public static HookRegistration onPlayerLogout(Priority priority, PlayerHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerLogout(priority, listener);
    }

    public static HookRegistration onPlayerClone(PlayerCloneHandler listener) {
        return onPlayerClone(Priority.NORMAL, listener);
    }

    public static HookRegistration onPlayerClone(Priority priority, PlayerCloneHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerClone(priority, listener);
    }

    public static HookRegistration onPlayerRespawn(PlayerRespawnHandler listener) {
        return onPlayerRespawn(Priority.NORMAL, listener);
    }

    public static HookRegistration onPlayerRespawn(Priority priority, PlayerRespawnHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerRespawn(priority, listener);
    }

    public static HookRegistration onPlayerChangedDimension(PlayerDimensionHandler listener) {
        return onPlayerChangedDimension(Priority.NORMAL, listener);
    }

    public static HookRegistration onPlayerChangedDimension(Priority priority, PlayerDimensionHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerPlayerChangedDimension(priority, listener);
    }

    public static HookRegistration onDatapackSync(DatapackSyncHandler listener) {
        return onDatapackSync(Priority.NORMAL, listener);
    }

    public static HookRegistration onDatapackSync(Priority priority, DatapackSyncHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerDatapackSync(priority, listener);
    }

    public static HookRegistration onChat(ChatHandler listener) {
        return onChat(Priority.NORMAL, listener);
    }

    public static HookRegistration onChat(Priority priority, ChatHandler listener) {
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(listener, "listener");
        return KineticServerEventRuntime.registerChat(priority, listener);
    }
}
