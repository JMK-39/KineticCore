package dev.xyat.kineticcore.feature.flight.mixin;

import dev.xyat.kineticcore.api.flight.KineticFlight;
import dev.xyat.kineticcore.api.flight.KineticFlightSources;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.feature.flight.FlightAbilityPackets;
import dev.xyat.kineticcore.feature.flight.FlightState;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
//? if >=1.20.2
/*import net.minecraft.server.network.ServerCommonPacketListenerImpl;*/
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class FlightServerMixins {

    @Mixin(Player.class)
    public static abstract class PlayerAbilityTweaks {
        @Inject(method = "onUpdateAbilities", at = @At("HEAD"))
        private void kineticcore$guardFlightState(CallbackInfo ci) {
            Player self = (Player) (Object) this;
            if (self.level().isClientSide) return;
            if (KineticFlightSources.allowsFlight(self) && !self.getAbilities().mayfly) {
                self.getAbilities().mayfly = true;
                if (FlightState.lastKnownFlying(self) && !FlightState.isProcessingExplicitCancel) {
                    self.getAbilities().flying = true;
                }
            }
        }

        // Noclip keeps the standing eye height in any pose. 1.20.1 asks Player for it; since 1.20.5 it is part of the
        // pose dimensions, so only their eye height is replaced and the hitbox stays as it is. Entity's constructor asks
        // as well, before abilities and the server game mode exist, so isCreative() cannot be used here.
        //? if >=1.20.5 {
        /*@Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
        private void kineticcore$getEyeHeight(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
            Player player = (Player) (Object) this;
            if (kineticcore$creativeNoclip(player)) {
                cir.setReturnValue(cir.getReturnValue().withEyeHeight(1.62F));
            }
        }
        *///?} else {
        @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
        private void kineticcore$getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
            Player player = (Player) (Object) this;
            if (kineticcore$creativeNoclip(player)) {
                cir.setReturnValue(1.62F);
            }
        }
        //?}

        @Unique
        private static boolean kineticcore$creativeNoclip(Player player) {
            return KineticFlight.noclipEnabled(player) && player.getAbilities() != null && player.getAbilities().instabuild;
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerTweaks {
        @Unique private boolean kineticcore$wasFlyingBeforeGamemode;

        @Inject(method = "restoreFrom", at = @At("TAIL"))
        private void kineticcore$onClone(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
            KineticFlight.copyPersistentState(oldPlayer, (ServerPlayer) (Object) this);
        }

        @Inject(method = "setGameMode", at = @At("HEAD"))
        private void kineticcore$captureFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            this.kineticcore$wasFlyingBeforeGamemode = ((ServerPlayer) (Object) this).getAbilities().flying;
        }

        @Inject(method = "setGameMode", at = @At("TAIL"))
        private void kineticcore$restoreFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            ServerPlayer self = (ServerPlayer) (Object) this;

            if (gameType != GameType.CREATIVE) {
                KineticFlight.applyServerNoclip(self, false);
            } else {
                KineticFlight.syncServerNoclip(self);
            }

            if (KineticFlightSources.allowsFlight(self)) {
                self.getAbilities().mayfly = true;
                if (this.kineticcore$wasFlyingBeforeGamemode) self.getAbilities().flying = true;
                FlightState.isInternalUpdate = true;
                self.onUpdateAbilities();
                FlightState.isInternalUpdate = false;
            }
        }
    }

    @Mixin(ServerGamePacketListenerImpl.class)
    public static abstract class NetworkTweaks {
        @Accessor("player")
        public abstract ServerPlayer kineticcore$getPlayer();

        @Accessor("clientIsFloating")
        public abstract void kineticcore$setClientIsFloating(boolean floating);

        @Accessor("aboveGroundTickCount")
        public abstract void kineticcore$setAboveGroundTickCount(int tickCount);

        @Inject(method = "tick", at = @At("HEAD"))
        private void kineticcore$allowAuthoritativeSuperFlight(CallbackInfo ci) {
            if (KineticSuperFlight.active(this.kineticcore$getPlayer())) {
                this.kineticcore$setClientIsFloating(false);
                this.kineticcore$setAboveGroundTickCount(0);
            }
        }

        //? if <1.20.2 {
        @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$interceptOutboundAbilities(Packet<?> packet, net.minecraft.network.PacketSendListener listener, CallbackInfo ci) {
            FlightAbilityPackets.interceptOutbound(this.kineticcore$getPlayer(), packet, ci);
        }
        //?}

        @Inject(method = "handlePlayerAbilities", at = @At("HEAD"))
        private void kineticcore$onHandleAbilitiesStart(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
            if (!packet.isFlying()) FlightState.isProcessingExplicitCancel = true;
            FlightState.setLastKnownFlying(this.kineticcore$getPlayer(), packet.isFlying());
        }

        @Inject(method = "handlePlayerAbilities", at = @At("TAIL"))
        private void kineticcore$onHandleAbilitiesEnd(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
            FlightState.isProcessingExplicitCancel = false;
        }

        @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
        private void kineticcore$bypassBlockCollisionCheck(LevelReader level, AABB aabb, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
            ServerPlayer player = this.kineticcore$getPlayer();
            if (player.isCreative() && KineticFlight.noclipEnabled(player)) cir.setReturnValue(false);
        }

        @ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F), require = 0)
        private float kineticcore$disableSpeedCheck(float original) { return Float.MAX_VALUE; }

        @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100.0D), require = 0)
        private double kineticcore$disableVehicleCheck(double original) { return Double.MAX_VALUE; }
    }

    // Outbound ability packets go through the game packet listener before 1.20.2 and through the listener shared by
    // the configuration and game phases since.
    //? if >=1.20.2 {
    /*@Mixin(ServerCommonPacketListenerImpl.class)
    public static abstract class CommonNetworkTweaks {
        @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$interceptOutboundAbilities(Packet<?> packet, net.minecraft.network.PacketSendListener listener, CallbackInfo ci) {
            if ((Object) this instanceof ServerGamePacketListenerImpl game) FlightAbilityPackets.interceptOutbound(game.player, packet, ci);
        }
    }
    *///?}
}
