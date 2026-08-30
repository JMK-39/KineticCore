package dev.xyat.kineticcore.feature.flight.mixin.client;

import net.minecraft.ChatFormatting;
import com.mojang.authlib.GameProfile;
import dev.xyat.kineticcore.feature.flight.client.FlightClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
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
                float targetSpeed = FlightClient.storedFlightMultiplier * 0.05F;
                if (Math.abs(self.getAbilities().getFlyingSpeed() - targetSpeed) > 0.0001F) {
                    self.getAbilities().setFlyingSpeed(targetSpeed);
                }
                if (FlightClient.noclipEnabled) {
                    self.noPhysics = true;
                    self.setOnGround(false);
                    if (self.getBbWidth() > 0.01F) self.refreshDimensions();
                    if (!self.getAbilities().flying) { self.getAbilities().flying = true; self.onUpdateAbilities(); }
                }
            } else {
                if (FlightClient.noclipEnabled) {
                    FlightClient.setNoclip(false);
                }
                if (self.noPhysics && !self.isSpectator()) {
                    self.noPhysics = false;
                    self.refreshDimensions();
                }
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

            // 仅在客户端生效，关闭了惯性，正在飞行，且不是骑乘状态
            if (self.level().isClientSide && !FlightClient.inertiaEnabled && self.getAbilities().flying && !self.isPassenger()) {

                // 1. 获取飞行速度，完美适配滚轮调速功能
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

        @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
        private void kineticcore$creativeScrollSpeed(long pWindowPointer, double pXOffset, double pYOffset, CallbackInfo ci) {
            LocalPlayer player = this.minecraft.player;

            if (player != null && player.isCreative() && FlightClient.SPEED_MOD_KEY.isDown()) {
                float currentMult = FlightClient.storedFlightMultiplier;
                float step = Screen.hasAltDown() ? ((pYOffset > 0) ? 1.0F : -1.0F) : ((pYOffset > 0) ? 0.1F : -0.1F);
                float newMult = Mth.clamp(currentMult + step, 0.1F, 100.0F);
                newMult = Math.round(newMult * 10.0F) / 10.0F;

                if (newMult != currentMult) {
                    FlightClient.storedFlightMultiplier = newMult;
                    player.getAbilities().setFlyingSpeed(newMult * 0.05F);
                    player.onUpdateAbilities();

                    String displayVal = (newMult == (int)newMult) ? String.valueOf((int)newMult) : String.format("%.1f", newMult);
                    player.displayClientMessage(Component.translatable("gui.kineticcore.flying.speed", Component.literal(displayVal).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)), true);
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
                player.getAbilities().setFlyingSpeed(FlightClient.storedFlightMultiplier * 0.05F);
                if (this.kineticcore$wasFlying) {
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                }
                if (FlightClient.noclipEnabled) {
                    player.noPhysics = true;
                    player.refreshDimensions();
                }
            } else {
                if (FlightClient.noclipEnabled) {
                    FlightClient.setNoclip(false);
                }
                player.noPhysics = false;
                player.refreshDimensions();
            }
        }
    }

    @Mixin(Camera.class)
    public static class CameraTweaks {
        @Shadow private Entity entity;

        @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
        private void kineticcore$allowCameraThroughBlocks(double startingDistance, CallbackInfoReturnable<Double> cir) {
            if (this.entity instanceof LocalPlayer && FlightClient.noclipEnabled) {
                cir.setReturnValue(startingDistance);
            }
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
            return originalIsSpectator || FlightClient.noclipEnabled;
        }
    }
}