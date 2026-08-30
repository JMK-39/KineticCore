package dev.xyat.kineticcore.feature.flight.mixin;

import dev.xyat.kineticcore.feature.flight.api.FlightAPI;
import dev.xyat.kineticcore.feature.flight.client.FlightClient;
import dev.xyat.kineticcore.feature.flight.network.FlightNetwork;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Objects;

public class FlightServerMixins {

    @Mixin(Player.class)
    public static abstract class PlayerAbilityTweaks {
        @Inject(method = "onUpdateAbilities", at = @At("HEAD"))
        private void kineticcore$guardFlightState(CallbackInfo ci) {
            Player self = (Player) (Object) this;
            if (self.level().isClientSide) return;
            if (FlightAPI.shouldForceAllowFlight(self) && !self.getAbilities().mayfly) {
                self.getAbilities().mayfly = true;
                if (FlightAPI.getLastKnownFlying(self) && !FlightAPI.isProcessingExplicitCancel) {
                    self.getAbilities().flying = true;
                }
            }
        }

        @Inject(method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", at = @At("HEAD"), cancellable = true)
        private void kineticcore$getDimensionsForPose(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
            Player player = (Player) (Object) this;
            boolean isActive = player.level().isClientSide ? FlightClient.noclipEnabled : player.getPersistentData().getBoolean("kt_noclip");
            if (player.isCreative() && isActive) {
                cir.setReturnValue(EntityDimensions.scalable(0.0F, 0.0F));
            }
        }

        @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("HEAD"), cancellable = true)
        private void kineticcore$getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
            Player player = (Player) (Object) this;
            boolean isActive = player.level().isClientSide ? FlightClient.noclipEnabled : player.getPersistentData().getBoolean("kt_noclip");
            if (player.isCreative() && isActive) {
                cir.setReturnValue(1.62F);
            }
        }
    }

    @Mixin(ServerPlayer.class)
    public static abstract class ServerPlayerTweaks {
        @Unique private boolean kineticcore$wasFlyingBeforeGamemode = false;

        @Inject(method = "restoreFrom", at = @At("TAIL"))
        private void kineticcore$onClone(ServerPlayer oldPlayer, boolean wonGame, CallbackInfo ci) {
            ServerPlayer self = (ServerPlayer) (Object) this;
            if (oldPlayer.getPersistentData().contains("flight_sources_list")) {
                self.getPersistentData().put("flight_sources_list", Objects.requireNonNull(oldPlayer.getPersistentData().get("flight_sources_list")).copy());
            }
            if (oldPlayer.getAbilities().flying) self.getAbilities().flying = true;
            boolean noclip = oldPlayer.getPersistentData().getBoolean("kt_noclip");
            self.getPersistentData().putBoolean("kt_noclip", noclip);
            self.noPhysics = noclip;
            self.refreshDimensions();
        }

        @Inject(method = "setGameMode", at = @At("HEAD"))
        private void kineticcore$captureFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            FlightAPI.isGamemodeSwitching = true;
            this.kineticcore$wasFlyingBeforeGamemode = ((ServerPlayer)(Object)this).getAbilities().flying;
        }

        @Inject(method = "setGameMode", at = @At("TAIL"))
        private void kineticcore$restoreFlightState(GameType gameType, CallbackInfoReturnable<Boolean> cir) {
            ServerPlayer self = (ServerPlayer) (Object) this;

            if (gameType != GameType.CREATIVE) {
                FlightNetwork.applyServerNoclip(self, false);
            } else {
                FlightNetwork.syncNoclipState(self);
            }

            if (FlightAPI.shouldForceAllowFlight(self)) {
                self.getAbilities().mayfly = true;
                if (this.kineticcore$wasFlyingBeforeGamemode) self.getAbilities().flying = true;
                FlightAPI.isInternalUpdate = true;
                self.onUpdateAbilities();
                FlightAPI.isInternalUpdate = false;
            }
            FlightAPI.isGamemodeSwitching = false;
        }
    }

    @Mixin(ServerGamePacketListenerImpl.class)
    public static class NetworkTweaks {
        @Shadow public ServerPlayer player;

        @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
        private void kineticcore$interceptOutboundAbilities(Packet<?> packet, net.minecraft.network.PacketSendListener listener, CallbackInfo ci) {
            if (packet instanceof ClientboundPlayerAbilitiesPacket) {
                if (FlightAPI.isInternalUpdate || FlightAPI.isProcessingExplicitCancel) return;
                boolean outgoingMayfly = this.player.getAbilities().mayfly;
                boolean wasFlying = FlightAPI.getLastKnownFlying(this.player);

                if (outgoingMayfly && !this.player.getAbilities().flying && wasFlying) {
                    ci.cancel(); this.player.getAbilities().flying = true; FlightAPI.isInternalUpdate = true;
                    this.player.onUpdateAbilities(); FlightAPI.isInternalUpdate = false; return;
                }
                if (!outgoingMayfly && FlightAPI.shouldForceAllowFlight(this.player)) {
                    ci.cancel(); this.player.getAbilities().mayfly = true; if (wasFlying) this.player.getAbilities().flying = true;
                    FlightAPI.isInternalUpdate = true; this.player.onUpdateAbilities(); FlightAPI.isInternalUpdate = false;
                }
            }
        }

        @Inject(method = "handlePlayerAbilities", at = @At("HEAD"))
        private void kineticcore$onHandleAbilitiesStart(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
            if (!packet.isFlying()) FlightAPI.isProcessingExplicitCancel = true;
            FlightAPI.setLastKnownFlying(this.player, packet.isFlying());
        }

        @Inject(method = "handlePlayerAbilities", at = @At("TAIL"))
        private void kineticcore$onHandleAbilitiesEnd(ServerboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
            FlightAPI.isProcessingExplicitCancel = false;
        }

        @Inject(method = "isPlayerCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
        private void kineticcore$bypassBlockCollisionCheck(LevelReader level, AABB aabb, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
            if (this.player.isCreative() && this.player.getPersistentData().getBoolean("kt_noclip")) cir.setReturnValue(false);
        }

        @ModifyConstant(method = "handleMovePlayer", constant = @Constant(floatValue = 100.0F), require = 0)
        private float kineticcore$disableSpeedCheck(float original) { return Float.MAX_VALUE; }

        @ModifyConstant(method = "handleMoveVehicle", constant = @Constant(doubleValue = 100.0D), require = 0)
        private double kineticcore$disableVehicleCheck(double original) { return Double.MAX_VALUE; }
    }
}