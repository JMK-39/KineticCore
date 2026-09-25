package dev.xyat.kineticcore.internal.mixin.api.setspawn;

import com.mojang.authlib.GameProfile;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
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

        @Accessor("server")
        public abstract MinecraftServer kineticcore$getServer();

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
            return KineticServerHookRuntime.globalSpawn(this.kineticcore$getServer())
                    .map(spawn -> spawn.getFirst().dimension())
                    .orElse(Level.OVERWORLD);
        }

        @Inject(method = "getPlayerForLogin", at = @At("TAIL"), cancellable = true)
        private void kineticcore$createNewPlayerInCustomSpawnLevel(GameProfile profile, CallbackInfoReturnable<ServerPlayer> cir) {
            KineticServerHookRuntime.createFreshLoginPlayer(this.kineticcore$getServer(), profile)
                    .ifPresent(cir::setReturnValue);
        }

        @Redirect(
                method = "placeNewPlayer",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"
                )
        )
        private void kineticcore$addNewPlayerAtExactSafeSpawn(ServerLevel level, ServerPlayer player) {
            Optional<KineticServerHookRuntime.FreshLoginPlacement> selected =
                    KineticServerHookRuntime.selectFreshLoginPlacement(this.kineticcore$getServer(), level, player);
            if (selected.isPresent()) {
                KineticServerHookRuntime.FreshLoginPlacement placement = selected.get();
                placement.level().addNewPlayer(player);
                KineticServerHookRuntime.finishFreshLoginPlacement(placement, player);
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

            return KineticServerHookRuntime.selectRespawnLevel(server).orElseGet(server::overworld);
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
            if (KineticServerHookRuntime.prepareFreshLoginPlacement(level.getServer(), player)) {
                ci.cancel();
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
            KineticServerHookRuntime.sharedSpawn(level.getServer(), level)
                    .ifPresent(cir::setReturnValue);
        }
    }
}
