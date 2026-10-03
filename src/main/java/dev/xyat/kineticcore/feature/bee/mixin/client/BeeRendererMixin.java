package dev.xyat.kineticcore.feature.bee.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if >=26.1 {
/*import net.minecraft.client.renderer.entity.BeeRenderer;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
*///?} else {
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class BeeRendererMixin {

    //? if >=26.1 {
    /*// 26.1 renderers draw from render states; the flip angle belongs to the renderer and the scale to the state.
    // 修复蜜蜂的翻转角度为 180 度
    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true)
    protected void kineticcore$fixBeeFlip(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof BeeRenderer) {
            cir.setReturnValue(180.0F);
        }
    }

    // 缩小蜜蜂的渲染体积，叠加在原版幼年蜜蜂的缩放之上
    @Inject(method = "scale", at = @At("TAIL"))
    protected void kineticcore$scaleBeeVisuals(LivingEntityRenderState state, PoseStack poseStack, CallbackInfo ci) {
        if (state instanceof BeeRenderState) {
            poseStack.scale(0.25F, 0.25F, 0.25F);
        }
    }
    *///?} else {
    // 修复蜜蜂的翻转角度为 180 度
    @Inject(method = "getFlipDegrees", at = @At("HEAD"), cancellable = true)
    protected void kineticcore$fixBeeFlip(LivingEntity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof Bee) {
            cir.setReturnValue(180.0F);
        }
    }

    // 缩小蜜蜂的渲染体积
    @Inject(method = "scale", at = @At("TAIL"))
    protected void kineticcore$scaleBeeVisuals(LivingEntity entity, PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (entity instanceof Bee) {
            // 这里的三个参数分别对应 X, Y, Z 轴的缩放比例。0.25F 即为原来的 25% 体型。
            // 注入在 TAIL 处的好处是：它会叠加原版幼年蜜蜂的缩放逻辑，使“幼年蜜蜂”变得更小。
            poseStack.scale(0.25F, 0.25F, 0.25F);
        }
    }
    //?}
}
