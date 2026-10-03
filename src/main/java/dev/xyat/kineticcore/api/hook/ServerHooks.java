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

/**
 * Hooks into server behavior that Forge has no event for: spawn placement, world deletion and data-pack order.
 *
 * <p>Handlers are called in registration order on the server thread (world deletion on the client thread, from the
 * world selection screen). A handler that throws is logged and skipped, so one broken add-on cannot block the
 * others or the vanilla fallback. Close the returned {@link HookRegistration} to unregister.
 */
public final class ServerHooks {
    private ServerHooks() {
    }

    /**
     * 注册出生点处理器。无返回值通知按注册顺序执行并隔离单个处理器异常；首次登录处理器的 识别、精确放置准备和完成清理也统一经过 Runtime 分发，不由 Mixin 直接调用处理器。 返回 Optional
     * 的出生点选择保持优先命中；异常或无效的查询结果会被记录并跳过， 全部处理器均未命中时回退到原版出生点。
     *
     * <p>Registers a spawn override. Notifications run in registration order with each handler's failures isolated;
     * for queries returning {@link java.util.Optional} the first handler with a valid answer wins, broken or
     * invalid answers are logged and skipped, and vanilla spawning is used when no handler answers.
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

    /**
     * Replaces where players spawn, first log in and respawn. Every method has a no-op default; implement only the
     * steps you need. For queries that return an {@link Optional}, the first handler that returns a value wins and
     * an empty result falls back to the next handler, then to vanilla.
     */
    public interface SpawnOverride {
        /**
         * Called before the server prepares spawn chunks at startup, for example to apply a cached spawn so the
         * right chunks are loaded.
         */
        default void beforePrepareLevels(MinecraftServer server) {
        }

        /**
         * Returns the dimension new players are placed in instead of the Overworld.
         *
         * @return the spawn level and position, or empty to keep vanilla
         */
        default Optional<Pair<ServerLevel, BlockPos>> globalSpawn(MinecraftServer server) {
            return Optional.empty();
        }

        //? if >=26.1 {
        /*/^*
         * Reserves a custom placement for a player logging in for the first time. 26.1 creates the player itself, in
         * the dimension of the world spawn data; the reserved placement is applied when the player joins the level.
         *
         * @return whether a placement was reserved
         ^/
        default boolean prepareFreshLogin(MinecraftServer server, java.util.UUID playerId, String playerName) {
            return false;
        }
        *///?} else {
        /**
         * Creates the player object for a login when the player must be constructed in a custom spawn level.
         *
         * @return the new player, or empty to let vanilla create it
         */
        default Optional<ServerPlayer> createFreshLoginPlayer(MinecraftServer server, GameProfile profile) {
            return Optional.empty();
        }
        //?}

        /**
         * Returns whether this handler places the given player; only players that never joined before should return
         * {@code true}.
         */
        default boolean isFreshLoginPlayer(ServerPlayer player) {
            return false;
        }

        /**
         * Moves a fresh player to their exact spawn before they are added to the world. Returning a level also
         * cancels vanilla's random spawn-area offset.
         *
         * @return the level to add the player to, or empty to keep the level vanilla chose
         */
        default Optional<ServerLevel> ensureFreshPlayerPlacement(MinecraftServer server, ServerPlayer player) {
            return Optional.empty();
        }

        /** Called after a fresh player placed by this handler was added to the world, to clear pending state. */
        default void finishFreshPlayerPlacement(ServerPlayer player) {
        }

        /**
         * Discards a respawn position stored by {@link #markPendingRespawnPlacement(Pair)}; called when the respawn
         * does not use it.
         */
        default void clearPendingRespawnPlacement() {
        }

        /**
         * Returns where a player without a bed or respawn anchor respawns. Not called when returning from the End.
         *
         * @return the respawn level and position, or empty to keep vanilla
         */
        default Optional<Pair<ServerLevel, BlockPos>> respawnSpawn(MinecraftServer server) {
            return Optional.empty();
        }

        /**
         * Remembers the position returned by {@link #respawnSpawn(MinecraftServer)} until the respawned player
         * exists.
         */
        default void markPendingRespawnPlacement(Pair<ServerLevel, BlockPos> spawn) {
        }

        /** Moves the new player object to the remembered respawn position just before it is added to the world. */
        default void applyPendingRespawnPlacement(ServerPlayer player) {
        }

        /** Called after the respawn finished, for example to resend the position to the client. */
        default void syncAppliedRespawnPlacement(ServerPlayer player) {
        }

        /** Called when a level's world spawn changes, for example through {@code /setworldspawn}. */
        default void onDefaultSpawnChanged(ServerLevel level, BlockPos pos, float angle) {
        }

        /**
         * Returns an exact world spawn position for a level instead of the stored one.
         *
         * @return the position, or empty to keep vanilla
         */
        default Optional<BlockPos> sharedSpawn(MinecraftServer server, ServerLevel level) {
            return Optional.empty();
        }
    }

    /**
     * Moves singleplayer worlds to a recycle bin instead of deleting them. If a handler throws while deciding, the
     * world is kept rather than deleted permanently.
     */
    public interface WorldDeletionHandler {
        /** Returns whether this handler takes over deleting the world at {@code worldPath}. */
        boolean shouldRecycle(Path worldPath);

        /**
         * Moves the world away instead of deleting it. The world's storage lock is already released.
         *
         * @throws Exception if the world could not be moved; the error is logged and the world is left in place
         */
        void recycle(Path worldPath) throws Exception;
    }

    /**
     * Supplies a preferred order for enabled data packs. Packs listed by providers load in that order; unlisted
     * packs keep their vanilla position.
     */
    public interface DataPackOrderProvider {
        /** Called before {@link #order()} each time the pack list is rebuilt, to reload the configured order. */
        void refresh();

        /**
         * Returns pack ids in the desired load order. Unknown ids are ignored.
         *
         * @return the ids, or {@code null} for no preference
         */
        List<String> order();
    }
}
