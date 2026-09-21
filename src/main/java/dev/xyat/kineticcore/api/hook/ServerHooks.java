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

/** Public API type for server hooks. */
public final class ServerHooks {
    private ServerHooks() {
    }

    /**
     * 注册出生点处理器。无返回值通知按注册顺序执行并隔离单个处理器异常；首次登录处理器的
     * 识别、精确放置准备和完成清理也统一经过 Runtime 分发，不由 Mixin 直接调用处理器。
     * 返回 Optional 的出生点选择保持优先命中；异常或无效的查询结果会被记录并跳过，
     * 全部处理器均未命中时回退到原版出生点。
     */
    public static HookRegistration onSpawnOverride(SpawnOverride handler) {
        return KineticServerHookRuntime.registerSpawnOverride(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a world recycle handler. A failed selection must never cause
     * an irreversible fallback to vanilla deletion; later successful handlers
     * are still allowed to take over.
     */
    public static HookRegistration onWorldDeletion(WorldDeletionHandler handler) {
        return KineticServerHookRuntime.registerWorldDeletion(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /**
     * Registers a data pack order provider. Providers are queried in order;
     * a failed provider is logged and skipped without suppressing later ones.
     */
    public static HookRegistration onDataPackOrder(DataPackOrderProvider handler) {
        return KineticServerHookRuntime.registerDataPackOrder(
                Objects.requireNonNull(handler, "handler")
        );
    }

    /** Public API contract for spawn override. */
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

    /** Callback contract for world deletion notifications. */
    public interface WorldDeletionHandler {
        boolean shouldRecycle(Path worldPath);

        void recycle(Path worldPath) throws Exception;
    }

    /** Provider contract for data pack order data. */
    public interface DataPackOrderProvider {
        void refresh();

        List<String> order();
    }
}
