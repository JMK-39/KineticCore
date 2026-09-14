package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.hook.ServerHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KineticServerHookRuntime {
    private static final CopyOnWriteArrayList<ServerHooks.SpawnOverride> SPAWN_OVERRIDES =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ServerHooks.WorldDeletionHandler> WORLD_DELETION =
            new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ServerHooks.DataPackOrderProvider> DATA_PACK_ORDER =
            new CopyOnWriteArrayList<>();

    private KineticServerHookRuntime() {
    }

    public static HookRegistration registerSpawnOverride(ServerHooks.SpawnOverride handler) {
        SPAWN_OVERRIDES.add(handler);
        return () -> SPAWN_OVERRIDES.remove(handler);
    }

    public static HookRegistration registerWorldDeletion(ServerHooks.WorldDeletionHandler handler) {
        WORLD_DELETION.add(handler);
        return () -> WORLD_DELETION.remove(handler);
    }

    public static HookRegistration registerDataPackOrder(ServerHooks.DataPackOrderProvider handler) {
        DATA_PACK_ORDER.add(handler);
        return () -> DATA_PACK_ORDER.remove(handler);
    }

    public static List<ServerHooks.SpawnOverride> spawnOverrides() {
        return List.copyOf(SPAWN_OVERRIDES);
    }


    public static void beforePrepareLevels(MinecraftServer server) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            handler.beforePrepareLevels(server);
        }
    }

    public static void clearPendingRespawnPlacement() {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            handler.clearPendingRespawnPlacement();
        }
    }

    public static void applyPendingRespawnPlacement(ServerPlayer player) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            handler.applyPendingRespawnPlacement(player);
        }
    }

    public static void syncAppliedRespawnPlacement(ServerPlayer player) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            handler.syncAppliedRespawnPlacement(player);
        }
    }

    public static void onDefaultSpawnChanged(ServerLevel level, BlockPos pos, float angle) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            handler.onDefaultSpawnChanged(level, pos, angle);
        }
    }

    public static ServerHooks.WorldDeletionHandler worldDeletionHandler(Path worldPath) {
        for (ServerHooks.WorldDeletionHandler handler : WORLD_DELETION) {
            if (handler.shouldRecycle(worldPath)) return handler;
        }
        return null;
    }

    public static List<String> dataPackOrder() {
        Set<String> ordered = new LinkedHashSet<>();
        for (ServerHooks.DataPackOrderProvider provider : DATA_PACK_ORDER) {
            provider.refresh();
            List<String> order = provider.order();
            if (order != null) ordered.addAll(order);
        }
        return List.copyOf(ordered);
    }
}
