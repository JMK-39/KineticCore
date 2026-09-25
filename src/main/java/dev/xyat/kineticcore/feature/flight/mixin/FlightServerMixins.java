package dev.xyat.kineticcore.feature.flight.mixin;

import dev.xyat.kineticcore.api.flight.KineticFlightSources;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.feature.flight.FlightState;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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

        @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("HEAD"), cancellable = true)
        private void kineticcore$getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
            Player player = (Player) (Object) this;
            if (player.isCreative() && FlightState.noclipEnabled(player)) {
                cir.setReturnValue(1.62F);
            }
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerTweaks {
        @Unique private boolean kineticcore$wasFlyingBeforeGamemode;

        @Inject(method = "restoreFrom", at = @At("TAIL"))
        private void kineticcore$onClone(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
            FlightState.copyPersistentState(oldPlayer, (ServerPlayer) (Object) this);
        }

        @Inject(method = "setGameMode", at = @At("HEAD"))
        private void kineticcore$captureFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            FlightState.isGamemodeSwitching = true;
            this.kineticcore$wasFlyingBeforeGamemode = ((ServerPlayer) (Object) this).getAbilities().flying;
        }

        @Inject(method = "setGameMode", at = @At("TAIL"))
        private void kineticcore$restoreFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            ServerPlayer self = (ServerPlayer) (Object) this;

            if (gameType != GameType.CREATIVE) {
                FlightState.applyServerNoclip(self, false);
            } else {
                FlightState.syncServerNoclip(self);
            }

            if (KineticFlightSources.allowsFlight(self)) {
                self.getAbilities().mayfly = true;
                if (this.kineticcore$wasFlyingBeforeGamemode) self.getAbilities().flying = true;
                FlightState.isInternalUpdate = true;
                self.onUpdateAbilities();
                FlightState.isInternalUpdate = false;
            }
            FlightState.isGamemodeSwitching = false;
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

        @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$interceptOutboundAbilities(Packet<?> packet, net.minecraft.network.PacketSendListener listener, CallbackInfo ci) {
            if (!(packet instanceof ClientboundPlayerAbilitiesPacket)) return;
            if (FlightState.isInternalUpdate || KineticFlightSources.abilityRefreshInProgress() || FlightState.isProcessingExplicitCancel) return;

            ServerPlayer player = this.kineticcore$getPlayer();
            boolean outgoingMayfly = player.getAbilities().mayfly;
            boolean wasFlying = FlightState.lastKnownFlying(player);

            if (outgoingMayfly && !player.getAbilities().flying && wasFlying) {
                ci.cancel();
                player.getAbilities().flying = true;
                FlightState.isInternalUpdate = true;
                player.onUpdateAbilities();
                FlightState.isInternalUpdate = false;
                return;
            }
            if (!outgoingMayfly && KineticFlightSources.allowsFlight(player)) {
                ci.cancel();
                player.getAbilities().mayfly = true;
                if (wasFlying) player.getAbilities().flying = true;
                FlightState.isInternalUpdate = true;
                player.onUpdateAbilities();
                FlightState.isInternalUpdate = false;
            }
        }

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
            if (player.isCreative() && FlightState.noclipEnabled(player)) cir.setReturnValue(false);
        }

        @ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F), require = 0)
        private float kineticcore$disableSpeedCheck(float original) { return Float.MAX_VALUE; }

        @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100.0D), require = 0)
        private double kineticcore$disableVehicleCheck(double original) { return Double.MAX_VALUE; }
    }
}
