package dev.xyat.kineticcore.api.hook;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import dev.xyat.kineticcore.internal.runtime.KineticServerHookRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ServerHooks {
    private ServerHooks() {
    }

    public static HookRegistration onSpawnOverride(SpawnOverride handler) {
        return KineticServerHookRuntime.registerSpawnOverride(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onWorldDeletion(WorldDeletionHandler handler) {
        return KineticServerHookRuntime.registerWorldDeletion(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public static HookRegistration onDataPackOrder(DataPackOrderProvider handler) {
        return KineticServerHookRuntime.registerDataPackOrder(
                Objects.requireNonNull(handler, "handler")
        );
    }

    public interface SpawnOverride {
        default void beforePrepareLevels(MinecraftServer server) {
        }

        default Optional<Pair<ServerLevel, BlockPos>> globalSpawn(MinecraftServer server) {
            return Optional.empty();
        }

        default Optional<ServerPlayer> createFreshLoginPlayer(MinecraftServer server, GameProfile profile) {
            return Optional.empty();
        }

        default boolean isFreshLoginPlayer(ServerPlayer player) {
            return false;
        }

        default Optional<ServerLevel> ensureFreshPlayerPlacement(MinecraftServer server, ServerPlayer player) {
            return Optional.empty();
        }

        default void finishFreshPlayerPlacement(ServerPlayer player) {
        }

        default void clearPendingRespawnPlacement() {
        }

        default Optional<Pair<ServerLevel, BlockPos>> respawnSpawn(MinecraftServer server) {
            return Optional.empty();
        }

        default void markPendingRespawnPlacement(Pair<ServerLevel, BlockPos> spawn) {
        }

        default void applyPendingRespawnPlacement(ServerPlayer player) {
        }

        default void syncAppliedRespawnPlacement(ServerPlayer player) {
        }

        default void onDefaultSpawnChanged(ServerLevel level, BlockPos pos, float angle) {
        }

        default Optional<BlockPos> sharedSpawn(MinecraftServer server, ServerLevel level) {
            return Optional.empty();
        }
    }

    public interface WorldDeletionHandler {
        boolean shouldRecycle(Path worldPath);

        void recycle(Path worldPath) throws Exception;
    }

    public interface DataPackOrderProvider {
        void refresh();

        List<String> order();
    }
}
