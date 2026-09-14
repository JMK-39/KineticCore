package dev.xyat.kineticcore.internal.mixin.setspawn;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import dev.xyat.kineticcore.api.hook.ServerHooks;
import dev.xyat.kineticcore.internal.runtime.KineticServerHookRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

public class SetSpawnMixins {

    @Mixin(MinecraftServer.class)
    public static abstract class MinecraftServerMixin {
        @Inject(method = "prepareLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V", at = @At("HEAD"))
        private void kineticcore$applyCachedOverworldSpawnBeforeVanillaSpawnChunks(ChunkProgressListener progressListener, CallbackInfo ci) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            KineticServerHookRuntime.beforePrepareLevels(server);
        }
    }

    @Mixin(PlayerList.class)
    public static abstract class PlayerListMixin {

        @Shadow @Final private MinecraftServer server;

        @Redirect(
                method = "placeNewPlayer",
                allow = 2,
                require = 2,
                at = @At(
                        value = "FIELD",
                        target = "Lnet/minecraft/world/level/Level;OVERWORLD:Lnet/minecraft/resources/ResourceKey;",
                        opcode = Opcodes.GETSTATIC
                )
        )
        private ResourceKey<Level> kineticcore$redirectPlaceNewPlayerOverworldKey() {
            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                Optional<Pair<ServerLevel, BlockPos>> spawn = handler.globalSpawn(this.server);
                if (spawn.isPresent()) return spawn.get().getFirst().dimension();
            }
            return Level.OVERWORLD;
        }

        @Inject(method = "getPlayerForLogin", at = @At("TAIL"), cancellable = true)
        private void kineticcore$createNewPlayerInCustomSpawnLevel(GameProfile profile, CallbackInfoReturnable<ServerPlayer> cir) {
            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                Optional<ServerPlayer> player = handler.createFreshLoginPlayer(this.server, profile);
                if (player.isPresent()) {
                    cir.setReturnValue(player.get());
                    return;
                }
            }
        }

        @Redirect(
                method = "placeNewPlayer",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
                )
        )
        private void kineticcore$addNewPlayerAtExactSafeSpawn(ServerLevel level, ServerPlayer player) {
            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                if (!handler.isFreshLoginPlayer(player)) continue;
                ServerLevel targetLevel = handler.ensureFreshPlayerPlacement(this.server, player).orElse(level);
                targetLevel.addNewPlayer(player);
                handler.finishFreshPlayerPlacement(player);
                return;
            }
            level.addNewPlayer(player);
        }

        @Redirect(
                method = "respawn",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"
                )
        )
        private ServerLevel kineticcore$redirectRespawnOverworld(MinecraftServer server, ServerPlayer player, boolean keepEverything) {
            if (keepEverything) {
                KineticServerHookRuntime.clearPendingRespawnPlacement();
                return server.overworld();
            }

            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                Optional<Pair<ServerLevel, BlockPos>> spawn = handler.respawnSpawn(server);
                if (spawn.isPresent()) {
                    handler.markPendingRespawnPlacement(spawn.get());
                    return spawn.get().getFirst();
                }
                handler.clearPendingRespawnPlacement();
            }
            return server.overworld();
        }

        @Redirect(
                method = "respawn",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/level/ServerLevel;addRespawnedPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
                )
        )
        private void kineticcore$addRespawnedPlayerAtExactSafeSpawn(ServerLevel level, ServerPlayer player) {
            KineticServerHookRuntime.applyPendingRespawnPlacement(player);
            level.addRespawnedPlayer(player);
        }

        @Inject(method = "respawn", at = @At("RETURN"))
        private void kineticcore$syncRespawnReturn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
            ServerPlayer newPlayer = cir.getReturnValue();
            if (newPlayer != null) {
                KineticServerHookRuntime.syncAppliedRespawnPlacement(newPlayer);
            }
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerMixin {
        @Inject(method = "fudgeSpawnLocation(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$cancelVanillaFudgeForFreshCustomSpawn(ServerLevel level, CallbackInfo ci) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                if (handler.isFreshLoginPlayer(player)
                        && handler.ensureFreshPlayerPlacement(level.getServer(), player).isPresent()) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Mixin(ServerLevel.class)
    public static abstract class ServerLevelMixin {
        @Inject(method = "setDefaultSpawnPos", at = @At("HEAD"))
        private void kineticcore$catchCommandSetSpawn(BlockPos pos, float angle, CallbackInfo ci) {
            ServerLevel level = (ServerLevel) (Object) this;
            KineticServerHookRuntime.onDefaultSpawnChanged(level, pos, angle);
        }

        @Inject(method = "getSharedSpawnPos", at = @At("HEAD"), cancellable = true)
        private void kineticcore$useExactSavedCustomSharedSpawn(CallbackInfoReturnable<BlockPos> cir) {
            ServerLevel level = (ServerLevel) (Object) this;
            for (ServerHooks.SpawnOverride handler : KineticServerHookRuntime.spawnOverrides()) {
                Optional<BlockPos> pos = handler.sharedSpawn(level.getServer(), level);
                if (pos.isPresent()) {
                    cir.setReturnValue(pos.get());
                    return;
                }
            }
        }
    }
}
