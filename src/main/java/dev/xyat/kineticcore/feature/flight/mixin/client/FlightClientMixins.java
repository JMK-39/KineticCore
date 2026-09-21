package dev.xyat.kineticcore.feature.flight.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import com.mojang.authlib.GameProfile;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.feature.flight.config.SuperFlightClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

public class FlightClientMixins {

    // ==========================================
    // 1. 客户端玩家基础逻辑（负责穿墙和速度同步）
    // ==========================================
    @Mixin(LocalPlayer.class)
    public static abstract class LocalPlayerTweaks extends AbstractClientPlayer {
        public LocalPlayerTweaks(ClientLevel level, GameProfile profile) { super(level, profile); }

        @Inject(method = "aiStep", at = @At("HEAD"))
        private void kineticcore$enforceClientPhysics(CallbackInfo ci) {
            LocalPlayer self = (LocalPlayer) (Object) this;

            if (self.isCreative()) {
                float targetSpeed = KineticFlightClient.flightSpeedMultiplier() * 0.05F;
                if (Math.abs(self.getAbilities().getFlyingSpeed() - targetSpeed) > 0.0001F) {
                    self.getAbilities().setFlyingSpeed(targetSpeed);
                }
                if (KineticFlightClient.noclipEnabled()) {
                    self.noPhysics = true;
                    self.setOnGround(false);
                    if (self.getBbWidth() > 0.01F) self.refreshDimensions();
                    if (!self.getAbilities().flying) { self.getAbilities().flying = true; self.onUpdateAbilities(); }
                }
            } else {
                if (KineticFlightClient.noclipEnabled()) {
                    KineticFlightClient.requestNoclip(false);
                }
                if (self.noPhysics && !self.isSpectator()) {
                    self.noPhysics = false;
                    self.refreshDimensions();
                }
            }
        }

        @Inject(method = "aiStep", at = @At("TAIL"))
        private void kineticcore$maintainRealFallFlyingPose(CallbackInfo ci) {
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (KineticFlightClient.superFlightManeuvering()) {
                self.startFallFlying();
                self.fallDistance = 0.0F;
            }
        }
    }

    // ==========================================
    // 2. 核心物理重写（精准拦截 Player 的 travel 方法）
    // ==========================================
    @Mixin(Player.class)
    public static abstract class PlayerPhysicsTweaks extends LivingEntity {
        protected PlayerPhysicsTweaks(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }

        @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
        private void kineticcore$advancedZeroInertiaFlight(Vec3 travelVector, CallbackInfo ci) {
            Player self = (Player) (Object) this;

            if (self.level().isClientSide && KineticFlightClient.appliesSuperFlightTo(self) && !self.isPassenger()) {
                KineticFlightClient.applySuperFlightTravel(self);
                ci.cancel();
                return;
            }

            // 仅在客户端生效，关闭了惯性，正在飞行，且不是骑乘状态
            if (self.level().isClientSide && !KineticFlightClient.inertiaEnabled() && self.getAbilities().flying && !self.isPassenger()) {

                float baseSpeed = self.getAbilities().getFlyingSpeed();
                float sprintMod = self.isSprinting() ? 2.0F : 1.0F;

                // 乘以 20.0F 放大倍数，保持原始速度
                float actualSpeed = baseSpeed * sprintMod * 20.0F;

                // 2. 瞬间赋予极大的初始动量（消除起步加速延迟）
                self.moveRelative(actualSpeed, travelVector);

                Vec3 vel = self.getDeltaMovement();

                // 3. 执行移动碰撞。水平乘以 0.3，垂直乘以 1.33 提升上升/下降手感
                self.move(MoverType.SELF, vel.multiply(0.3D, 1.33D, 0.3D));

                // 4. 移动完毕后，瞬间抽干残余动量，防止滑行
                self.setDeltaMovement(vel.multiply(0.4D, 0.6D, 0.4D));

                // 5. 防止在空中累积跌落伤害
                self.fallDistance = 0.0F;

                // 6. 拦截取消原版的缓慢飞行和阻力计算
                ci.cancel();
            }
        }
    }

    // ==========================================
    // 3. 滚轮调速、穿墙渲染等其他功能
    // ==========================================
    @Mixin(MouseHandler.class)
    public static class MouseTweaks {
        @Shadow @Final private Minecraft minecraft;
        @Unique private float kineticcore$freeLookBeforeYaw;
        @Unique private float kineticcore$freeLookBeforePitch;
        @Unique private boolean kineticcore$captureFreeLook;

        @Inject(method = "turnPlayer", at = @At("HEAD"))
        private void kineticcore$beforeTurnPlayer(CallbackInfo ci) {
            LocalPlayer player = this.minecraft.player;
            this.kineticcore$captureFreeLook = player != null
                    && KineticFlightClient.appliesSuperFlightTo(player)
                    && KineticFlightClient.superFlightFreeLookDown();
            if (this.kineticcore$captureFreeLook) {
                this.kineticcore$freeLookBeforeYaw = player.getYRot();
                this.kineticcore$freeLookBeforePitch = player.getXRot();
            }
        }

        @Inject(method = "turnPlayer", at = @At("TAIL"))
        private void kineticcore$afterTurnPlayer(CallbackInfo ci) {
            if (!this.kineticcore$captureFreeLook) return;
            LocalPlayer player = this.minecraft.player;
            if (player == null) return;
            float yawDelta = Mth.wrapDegrees(player.getYRot() - this.kineticcore$freeLookBeforeYaw);
            float pitchDelta = player.getXRot() - this.kineticcore$freeLookBeforePitch;
            player.setYRot(this.kineticcore$freeLookBeforeYaw);
            player.setXRot(this.kineticcore$freeLookBeforePitch);
            KineticFlightClient.captureSuperFlightFreeLookDelta(player, yawDelta, pitchDelta);
        }

        @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
        private void kineticcore$creativeScrollSpeed(long pWindowPointer, double pXOffset, double pYOffset, CallbackInfo ci) {
            LocalPlayer player = this.minecraft.player;

            if (player != null && KineticFlightClient.superFlightActive() && KineticClientRuntime.shiftKeyDown()) {
                double currentMult = KineticFlightClient.superFlightSelectedSpeedMultiplier();
                double step = pYOffset > 0.0D ? 1.0D : -1.0D;
                double newMult = Mth.clamp(currentMult + step,
                        SuperFlightClientConfig.MIN_SELECTED_SPEED,
                        SuperFlightClientConfig.MAX_SELECTED_SPEED);
                if (Double.compare(newMult, currentMult) != 0) {
                    KineticFlightClient.setSuperFlightSelectedSpeedMultiplier(newMult);
                    SuperFlightClientConfig.setSelectedSpeed(newMult);
                    player.displayClientMessage(KineticText.translatable(
                            "msg.kineticcore.superflight.speed",
                            Component.literal(String.valueOf((int) newMult))
                    ), true);
                }
                ci.cancel();
                return;
            }

            if (player != null && player.isCreative() && KineticFlightClient.isSpeedModifierDown()) {
                float currentMult = KineticFlightClient.flightSpeedMultiplier();
                float step = KineticClientRuntime.altModifierDown() ? ((pYOffset > 0) ? 1.0F : -1.0F) : ((pYOffset > 0) ? 0.1F : -0.1F);
                float newMult = Mth.clamp(currentMult + step, 0.1F, 100.0F);
                newMult = Math.round(newMult * 10.0F) / 10.0F;

                if (newMult != currentMult) {
                    KineticFlightClient.setFlightSpeedMultiplier(newMult);
                    player.getAbilities().setFlyingSpeed(newMult * 0.05F);
                    player.onUpdateAbilities();

                    String displayVal = (newMult == (int)newMult) ? String.valueOf((int)newMult) : String.format("%.1f", newMult);
                    player.displayClientMessage(KineticText.translatable("gui.kineticcore.flying.speed", Component.literal(displayVal)), true);
                }
                ci.cancel();
            }
        }
    }

    @Mixin(MultiPlayerGameMode.class)
    public static class GameModeTweaks {
        @Shadow @Final private Minecraft minecraft;
        @Unique private boolean kineticcore$wasFlying;

        @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V", at = @At("HEAD"))
        private void kineticcore$beforeSetMode(GameType type, @Nullable GameType previousType, CallbackInfo ci) {
            if (this.minecraft.player != null) {
                this.kineticcore$wasFlying = this.minecraft.player.getAbilities().flying;
            }
        }

        @Inject(method = "setLocalMode(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V", at = @At("TAIL"))
        private void kineticcore$afterSetMode(GameType type, @Nullable GameType previousType, CallbackInfo ci) {
            LocalPlayer player = this.minecraft.player;
            if (player == null) return;
            if (type == GameType.CREATIVE) {
                player.getAbilities().setFlyingSpeed(KineticFlightClient.flightSpeedMultiplier() * 0.05F);
                if (this.kineticcore$wasFlying) {
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                }
                if (KineticFlightClient.noclipEnabled()) {
                    player.noPhysics = true;
                    player.refreshDimensions();
                }
            } else {
                if (KineticFlightClient.noclipEnabled()) {
                    KineticFlightClient.requestNoclip(false);
                }
                player.noPhysics = false;
                player.refreshDimensions();
            }
        }
    }

    @Mixin(Camera.class)
    public static abstract class CameraTweaks {
        @Shadow private Entity entity;

        @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
        private void kineticcore$allowCameraThroughBlocks(double startingDistance, CallbackInfoReturnable<Double> cir) {
            if (this.entity instanceof LocalPlayer && KineticFlightClient.noclipEnabled()) {
                cir.setReturnValue(startingDistance);
            }
        }

    }


    @Mixin(PlayerRenderer.class)
    public static class PlayerRendererTweaks {
        @Inject(method = "setupRotations", at = @At("TAIL"))
        private void kineticcore$applySuperFlightBodyPose(
                AbstractClientPlayer player,
                PoseStack poseStack,
                float ageInTicks,
                float rotationYaw,
                float partialTicks,
                CallbackInfo ci
        ) {
            if (!(player instanceof LocalPlayer localPlayer)) return;
            if (!KineticFlightClient.superFlightManeuvering() || !localPlayer.isFallFlying()) return;
            poseStack.mulPose(Axis.ZP.rotationDegrees(KineticFlightClient.superFlightRoll(partialTicks)));
        }
    }

    @Mixin(GameRenderer.class)
    public static class GameRendererTweaks {
        @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
        private void kineticcore$superFlightFov(Camera camera, float partialTick, boolean useConfiguredFov, CallbackInfoReturnable<Double> cir) {
            if (!useConfiguredFov || !KineticFlightClient.superFlightActive()) return;
            double boost = KineticFlightClient.superFlightFovBoost();
            if (boost > 0.001D) cir.setReturnValue(cir.getReturnValue() + boost);
        }
    }

    @Mixin(LevelRenderer.class)
    public static class LevelRendererTweaks {
        @ModifyVariable(
                method = "setupRender",
                at = @At("HEAD"),
                ordinal = 1,
                argsOnly = true
        )
        private boolean kineticcore$bypassOcclusionForNoclip(boolean originalIsSpectator) {
            return originalIsSpectator || KineticFlightClient.noclipEnabled();
        }
    }
}