package dev.xyat.kineticcore.internal.mixin.api.setspawn;

import com.mojang.authlib.GameProfile;
import dev.xyat.kineticcore.internal.runtime.KineticServerHookRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if <26.1
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
        //? if >=26.1 {
        /*@Inject(method = "prepareLevels()V", at = @At("HEAD"))
        private void kineticcore$applyCachedOverworldSpawnBeforeVanillaSpawnChunks(CallbackInfo ci) {
        *///?} else {
        @Inject(method = "prepareLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V", at = @At("HEAD"))
        private void kineticcore$applyCachedOverworldSpawnBeforeVanillaSpawnChunks(ChunkProgressListener progressListener, CallbackInfo ci) {
        //?}
            MinecraftServer server = (MinecraftServer) (Object) this;
            KineticServerHookRuntime.beforePrepareLevels(server);
        }

        //? if >=26.1 {
        /*// 26.1 keeps the world spawn, including its dimension, on the server. Setting the same spawn again changes
        // nothing; any other spawn is a new one (/setworldspawn, other mods).
        @Inject(method = "setRespawnData", at = @At("HEAD"))
        private void kineticcore$catchCommandSetSpawn(net.minecraft.world.level.storage.LevelData.RespawnData respawnData, CallbackInfo ci) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            if (respawnData.equals(server.getRespawnData())) return;
            ServerLevel level = server.getLevel(respawnData.dimension());
            if (level != null) KineticServerHookRuntime.onDefaultSpawnChanged(level, respawnData.pos(), respawnData.yaw());
        }
        *///?}
    }

    //? if >=26.1 {
    /*// 26.1 decides a joining player's level and position in PrepareSpawnTask, before the player exists. A player that
    // never joined gets its custom placement reserved here; it is applied when placeNewPlayer adds the player.
    @Mixin(net.minecraft.server.network.config.PrepareSpawnTask.class)
    public static abstract class PrepareSpawnTaskMixin {
        @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private MinecraftServer server;
        @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.server.players.NameAndId nameAndId;

        @Inject(method = "start", at = @At("HEAD"))
        private void kineticcore$reserveFreshLoginPlacement(java.util.function.Consumer<?> connection, CallbackInfo ci) {
            KineticServerHookRuntime.prepareFreshLogin(this.server, this.nameAndId.id(), this.nameAndId.name());
        }
    }

    *///?}
    @Mixin(PlayerList.class)
    public static abstract class PlayerListMixin {

        @Accessor("server")
        public abstract MinecraftServer kineticcore$getServer();

        // 26.1 creates the player in PrepareSpawnTask, in the dimension of the world spawn data that SetSpawnHandler
        // sets, so only the exact placement below is needed there.
        //? if <1.20.5 {
        // 1.20.1 reads Level.OVERWORLD once for missing player data and once for a missing dimension.
        @Redirect(
                method = "placeNewPlayer",
                allow = 2,
                require = 2,
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;OVERWORLD:Lnet/minecraft/resources/ResourceKey;", opcode = Opcodes.GETSTATIC)
        )
        private ResourceKey<Level> kineticcore$redirectPlaceNewPlayerOverworldKey() {
            return KineticServerHookRuntime.globalSpawn(this.kineticcore$getServer())
                    .map(spawn -> spawn.getFirst().dimension())
                    .orElse(Level.OVERWORLD);
        }
        //?} else if <26.1 {
        /*// 1.20.5+ reads Level.OVERWORLD once for both.
        @Redirect(
                method = "placeNewPlayer",
                allow = 1,
                require = 1,
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;OVERWORLD:Lnet/minecraft/resources/ResourceKey;", opcode = Opcodes.GETSTATIC)
        )
        private ResourceKey<Level> kineticcore$redirectPlaceNewPlayerOverworldKey() {
            return KineticServerHookRuntime.globalSpawn(this.kineticcore$getServer())
                    .map(spawn -> spawn.getFirst().dimension())
                    .orElse(Level.OVERWORLD);
        }
        *///?}

        //? if <1.20.2 {
        @Inject(method = "getPlayerForLogin", at = @At("TAIL"), cancellable = true)
        private void kineticcore$createNewPlayerInCustomSpawnLevel(GameProfile profile, CallbackInfoReturnable<ServerPlayer> cir) {
            KineticServerHookRuntime.createFreshLoginPlayer(this.kineticcore$getServer(), profile)
                    .ifPresent(cir::setReturnValue);
        }
        //?} else if <26.1 {
        /*@Inject(method = "getPlayerForLogin", at = @At("TAIL"), cancellable = true)
        private void kineticcore$createNewPlayerInCustomSpawnLevel(GameProfile profile, net.minecraft.server.level.ClientInformation clientInformation, CallbackInfoReturnable<ServerPlayer> cir) {
            KineticServerHookRuntime.createFreshLoginPlayer(this.kineticcore$getServer(), profile)
                    .ifPresent(cir::setReturnValue);
        }
        *///?}

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

        // Since 1.21 KineticRespawnEventRuntime does this through NeoForge's respawn position event.
        //? if <1.21 {
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
        //?}

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
        //? if >=1.21 {
        /*private void kineticcore$syncRespawnReturn(ServerPlayer oldPlayer, boolean keepEverything, net.minecraft.world.entity.Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        *///?} else {
        private void kineticcore$syncRespawnReturn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        //?}
            ServerPlayer newPlayer = cir.getReturnValue();
            if (newPlayer != null) {
                KineticServerHookRuntime.syncAppliedRespawnPlacement(newPlayer);
            }
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerMixin {
        //? if >=1.21 {
        /*// 1.21 replaced fudgeSpawnLocation with adjustSpawnLocation, whose result the constructor moves the player
        // to; keeping the prepared position there has the effect cancelling the fudge had before.
        @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
        private void kineticcore$cancelVanillaFudgeForFreshCustomSpawn(ServerLevel level, net.minecraft.core.BlockPos spawn, CallbackInfoReturnable<net.minecraft.core.BlockPos> cir) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (KineticServerHookRuntime.prepareFreshLoginPlacement(level.getServer(), player)) {
                cir.setReturnValue(player.blockPosition());
            }
        }
        *///?} else {
        @Inject(method = "fudgeSpawnLocation(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$cancelVanillaFudgeForFreshCustomSpawn(ServerLevel level, CallbackInfo ci) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (KineticServerHookRuntime.prepareFreshLoginPlacement(level.getServer(), player)) {
                ci.cancel();
            }
        }
        //?}
    }

    // 26.1: spawn changes are caught in MinecraftServerMixin, and the world spawn carries its own dimension.
    //? if <26.1 {
    @Mixin(ServerLevel.class)
    public static abstract class ServerLevelMixin {
        @Inject(method = "setDefaultSpawnPos", at = @At("HEAD"))
        private void kineticcore$catchCommandSetSpawn(BlockPos pos, float angle, CallbackInfo ci) {
            ServerLevel level = (ServerLevel) (Object) this;
            // Since 1.20.5 prepareLevels sets the current spawn again on every start to place the spawn chunks;
            // only an actual change (/setworldspawn, other mods) counts as a new spawn.
            if (pos.equals(level.getSharedSpawnPos()) && angle == level.getSharedSpawnAngle()) return;
            KineticServerHookRuntime.onDefaultSpawnChanged(level, pos, angle);
        }
    }
    //?}

    //? if <26.1 {
    // ServerLevel inherits getSharedSpawnPos from Level. Only a running server is asked, on its own thread: during
    // startup the custom spawn is not worked out yet.
    @Mixin(Level.class)
    public static abstract class LevelMixin {
        @Inject(method = "getSharedSpawnPos", at = @At("HEAD"), cancellable = true)
        private void kineticcore$useExactSavedCustomSharedSpawn(CallbackInfoReturnable<BlockPos> cir) {
            if (!((Object) this instanceof ServerLevel level)) return;
            MinecraftServer server = level.getServer();
            if (!server.isReady() || !server.isSameThread()) return;
            KineticServerHookRuntime.sharedSpawn(server, level).ifPresent(cir::setReturnValue);
        }
    }
    //?}
}
