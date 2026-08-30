package dev.xyat.kineticcore.feature.setspawn.mixin;

import dev.xyat.kineticcore.feature.setspawn.config.SetSpawnConfig;
import dev.xyat.kineticcore.feature.setspawn.data.SetSpawnData;
import dev.xyat.kineticcore.feature.setspawn.event.SetSpawnHandler;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
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
            SetSpawnHandler.applyCachedOverworldSpawnBeforeVanillaSpawnChunks((MinecraftServer) (Object) this);
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
            Optional<Pair<ServerLevel, BlockPos>> spawn = SetSpawnHandler.getOrCreateGlobalSpawn(this.server);
            return spawn
                    .map(pair -> pair.getFirst().dimension())
                    .orElse(Level.OVERWORLD);
        }

        @Inject(method = "getPlayerForLogin", at = @At("TAIL"), cancellable = true)
        private void kineticcore$createNewPlayerInCustomSpawnLevel(GameProfile profile, CallbackInfoReturnable<ServerPlayer> cir) {
            Optional<ServerPlayer> player = SetSpawnHandler.createFreshLoginPlayer(this.server, profile);
            player.ifPresent(cir::setReturnValue);
        }

        @Redirect(
                method = "placeNewPlayer",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
                )
        )
        private void kineticcore$addNewPlayerAtExactSafeSpawn(ServerLevel level, ServerPlayer player) {
            if (SetSpawnHandler.isFreshLoginPlayer(player)) {
                ServerLevel targetLevel = SetSpawnHandler.ensureFreshPlayerPlacement(this.server, player).orElse(level);
                targetLevel.addNewPlayer(player);
                SetSpawnHandler.finishFreshPlayerPlacement(player);
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
                SetSpawnHandler.clearPendingRespawnPlacement();
                return server.overworld();
            }

            Optional<Pair<ServerLevel, BlockPos>> spawn = SetSpawnHandler.getOrCreateRespawnSpawn(server);
            if (spawn.isEmpty()) {
                SetSpawnHandler.clearPendingRespawnPlacement();
                return server.overworld();
            }

            SetSpawnHandler.markPendingRespawnPlacement(spawn.get());
            return spawn.get().getFirst();
        }

        @Redirect(
                method = "respawn",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/level/ServerLevel;addRespawnedPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
                )
        )
        private void kineticcore$addRespawnedPlayerAtExactSafeSpawn(ServerLevel level, ServerPlayer player) {
            SetSpawnHandler.applyPendingRespawnPlacement(player);
            level.addRespawnedPlayer(player);
        }

        @Inject(method = "respawn", at = @At("RETURN"))
        private void kineticcore$syncRespawnReturn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
            ServerPlayer newPlayer = cir.getReturnValue();
            if (newPlayer == null) {
                return;
            }

            SetSpawnHandler.syncAppliedRespawnPlacement(newPlayer);
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerMixin {
        @Inject(method = "fudgeSpawnLocation(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$cancelVanillaFudgeForFreshCustomSpawn(ServerLevel level, CallbackInfo ci) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (!SetSpawnHandler.isFreshLoginPlayer(player)) return;

            Optional<ServerLevel> targetLevel = SetSpawnHandler.ensureFreshPlayerPlacement(level.getServer(), player);
            if (targetLevel.isPresent()) {
                ci.cancel();
            }
        }
    }

    @Mixin(ServerLevel.class)
    public static abstract class ServerLevelMixin {
        @Inject(method = "setDefaultSpawnPos", at = @At("HEAD"))
        private void kineticcore$catchCommandSetSpawn(BlockPos pos, float angle, CallbackInfo ci) {
            if (SetSpawnHandler.isInternalModifying) return;
            if (!SetSpawnConfig.enableCustomSpawn) return;

            ServerLevel level = (ServerLevel) (Object) this;
            MinecraftServer server = level.getServer();
            SetSpawnData data = SetSpawnData.get(server.overworld());

            data.setSetSpawnWorldChecked(true);
            data.setSetSpawnWorldEnabled(true);
            data.setSpawnCalculated(true);
            data.setAdminSpawn(true);
            data.setAutomaticSpawnWorld(false);
            data.setDataVersion(SetSpawnData.CURRENT_DATA_VERSION);
            data.setInitialized(true);
            data.setSpawnDim(level.dimension().location().toString());
            data.setSpawnX(pos.getX());
            data.setSpawnY(pos.getY());
            data.setSpawnZ(pos.getZ());
            data.setDirty();
        }

        @Inject(method = "getSharedSpawnPos", at = @At("HEAD"), cancellable = true)
        private void kineticcore$useExactSavedCustomSharedSpawn(CallbackInfoReturnable<BlockPos> cir) {
            ServerLevel level = (ServerLevel) (Object) this;
            Optional<BlockPos> pos = SetSpawnHandler.getSavedSpawnPosForLevel(level.getServer(), level);
            pos.ifPresent(cir::setReturnValue);
        }
    }
}
