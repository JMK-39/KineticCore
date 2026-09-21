package dev.xyat.kineticcore.internal.runtime;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import dev.xyat.kineticcore.api.hook.HookRegistration;
import dev.xyat.kineticcore.api.hook.ServerHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
        return HookRegistration.once(() -> SPAWN_OVERRIDES.remove(handler));
    }

    public static HookRegistration registerWorldDeletion(ServerHooks.WorldDeletionHandler handler) {
        WORLD_DELETION.add(handler);
        return HookRegistration.once(() -> WORLD_DELETION.remove(handler));
    }

    public static HookRegistration registerDataPackOrder(ServerHooks.DataPackOrderProvider handler) {
        DATA_PACK_ORDER.add(handler);
        return HookRegistration.once(() -> DATA_PACK_ORDER.remove(handler));
    }

    /** Selects the first fresh-login handler while isolating broken providers. */
    public static Optional<FreshLoginPlacement> selectFreshLoginPlacement(
            MinecraftServer server,
            ServerLevel fallbackLevel,
            ServerPlayer player
    ) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            try {
                if (!handler.isFreshLoginPlayer(player)) continue;
                Optional<ServerLevel> target = handler.ensureFreshPlayerPlacement(server, player);
                ServerLevel targetLevel = target == null ? fallbackLevel : target.orElse(fallbackLevel);
                return Optional.of(new FreshLoginPlacement(handler, targetLevel));
            } catch (RuntimeException failure) {
                KineticCallbackQueries.reportFailure(failure);
            }
        }
        return Optional.empty();
    }

    /** Runs fresh-login placement preparation without exposing hook handlers to Mixins. */
    public static boolean prepareFreshLoginPlacement(MinecraftServer server, ServerPlayer player) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            try {
                if (!handler.isFreshLoginPlayer(player)) continue;
                Optional<ServerLevel> target = handler.ensureFreshPlayerPlacement(server, player);
                if (target != null && target.isPresent()) return true;
            } catch (RuntimeException failure) {
                KineticCallbackQueries.reportFailure(failure);
            }
        }
        return false;
    }

    /** Completes the selected fresh-login handler without allowing cleanup failure to abort player placement. */
    public static void finishFreshLoginPlacement(FreshLoginPlacement placement, ServerPlayer player) {
        try {
            placement.handler().finishFreshPlayerPlacement(player);
        } catch (RuntimeException failure) {
            KineticCallbackQueries.reportFailure(failure);
        }
    }

    public record FreshLoginPlacement(ServerHooks.SpawnOverride handler, ServerLevel level) {
    }

    /** Picks the first valid spawn override; broken handlers cannot block the vanilla fallback. */
    public static Optional<Pair<ServerLevel, BlockPos>> globalSpawn(MinecraftServer server) {
        return KineticCallbackQueries.firstPresent(SPAWN_OVERRIDES, handler -> {
            Optional<Pair<ServerLevel, BlockPos>> result = handler.globalSpawn(server);
            return result == null ? Optional.empty()
                    : result.filter(spawn -> spawn.getFirst() != null && spawn.getSecond() != null);
        });
    }

    /** Picks the first login-player override while allowing later handlers after a failure. */
    public static Optional<ServerPlayer> createFreshLoginPlayer(MinecraftServer server, GameProfile profile) {
        return KineticCallbackQueries.firstPresent(SPAWN_OVERRIDES,
                handler -> handler.createFreshLoginPlayer(server, profile));
    }

    /** Picks the first shared-spawn override; an empty result keeps vanilla behavior. */
    public static Optional<BlockPos> sharedSpawn(MinecraftServer server, ServerLevel level) {
        return KineticCallbackQueries.firstPresent(SPAWN_OVERRIDES,
                handler -> handler.sharedSpawn(server, level));
    }

    /**
     * Selects and marks the first valid respawn override. Failed providers are
     * reported and asked to clear pending state before the next provider runs.
     */
    public static Optional<ServerLevel> selectRespawnLevel(MinecraftServer server) {
        for (ServerHooks.SpawnOverride handler : SPAWN_OVERRIDES) {
            try {
                Optional<Pair<ServerLevel, BlockPos>> optional = handler.respawnSpawn(server);
                if (optional != null && optional.isPresent()) {
                    Pair<ServerLevel, BlockPos> spawn = optional.get();
                    if (spawn.getFirst() != null && spawn.getSecond() != null) {
                        handler.markPendingRespawnPlacement(spawn);
                        return Optional.of(spawn.getFirst());
                    }
                }
            } catch (RuntimeException failure) {
                KineticCallbackQueries.reportFailure(failure);
            }
            try {
                handler.clearPendingRespawnPlacement();
            } catch (RuntimeException failure) {
                KineticCallbackQueries.reportFailure(failure);
            }
        }
        return Optional.empty();
    }

    public static void beforePrepareLevels(MinecraftServer server) {
        KineticCallbackBatch.runAll(SPAWN_OVERRIDES, handler -> handler.beforePrepareLevels(server));
    }

    public static void clearPendingRespawnPlacement() {
        KineticCallbackBatch.runAll(SPAWN_OVERRIDES, ServerHooks.SpawnOverride::clearPendingRespawnPlacement);
    }

    public static void applyPendingRespawnPlacement(ServerPlayer player) {
        KineticCallbackBatch.runAll(SPAWN_OVERRIDES, handler -> handler.applyPendingRespawnPlacement(player));
    }

    public static void syncAppliedRespawnPlacement(ServerPlayer player) {
        KineticCallbackBatch.runAll(SPAWN_OVERRIDES, handler -> handler.syncAppliedRespawnPlacement(player));
    }

    public static void onDefaultSpawnChanged(ServerLevel level, BlockPos pos, float angle) {
        KineticCallbackBatch.runAll(SPAWN_OVERRIDES, handler -> handler.onDefaultSpawnChanged(level, pos, angle));
    }

    /**
     * Selects the first willing recycle handler. When a provider fails and none
     * succeeds, callers must not fall back to irreversible vanilla deletion.
     */
    public static WorldDeletionSelection selectWorldDeletion(Path worldPath) {
        boolean failed = false;
        for (ServerHooks.WorldDeletionHandler handler : WORLD_DELETION) {
            try {
                if (handler.shouldRecycle(worldPath)) return new WorldDeletionSelection(handler, false);
            } catch (RuntimeException failure) {
                failed = true;
                KineticCallbackQueries.reportFailure(failure);
            }
        }
        return new WorldDeletionSelection(null, failed);
    }

    public static final class WorldDeletionSelection {
        private final ServerHooks.WorldDeletionHandler handler;
        private final boolean failed;

        private WorldDeletionSelection(ServerHooks.WorldDeletionHandler handler, boolean failed) {
            this.handler = handler;
            this.failed = failed;
        }

        public boolean failed() {
            return failed;
        }

        public boolean selected() {
            return handler != null;
        }

        public void recycle(Path worldPath) throws Exception {
            if (handler == null) {
                throw new IllegalStateException("No world deletion handler selected");
            }
            handler.recycle(worldPath);
        }
    }

    public static List<String> dataPackOrder() {
        Set<String> ordered = new LinkedHashSet<>();
        for (ServerHooks.DataPackOrderProvider provider : DATA_PACK_ORDER) {
            try {
                provider.refresh();
                List<String> order = provider.order();
                if (order != null) {
                    for (String id : order) {
                        if (id != null) ordered.add(id);
                    }
                }
            } catch (RuntimeException failure) {
                KineticCallbackQueries.reportFailure(failure);
            }
        }
        return List.copyOf(ordered);
    }
}
