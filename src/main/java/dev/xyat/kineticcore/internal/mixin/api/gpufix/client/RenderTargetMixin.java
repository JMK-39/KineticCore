package dev.xyat.kineticcore.internal.mixin.api.gpufix.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.xyat.kineticcore.internal.client.gpu.GpuMemLeakFixHandler;
//? if <26.1
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;
//? if >=26.1
/*import org.spongepowered.asm.mixin.Shadow;*/
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    //? if >=26.1 {
    /*@Shadow protected @javax.annotation.Nullable com.mojang.blaze3d.textures.GpuTexture colorTexture;
    @Shadow protected @javax.annotation.Nullable com.mojang.blaze3d.textures.GpuTextureView colorTextureView;
    @Shadow protected @javax.annotation.Nullable com.mojang.blaze3d.textures.GpuTexture depthTexture;
    @Shadow protected @javax.annotation.Nullable com.mojang.blaze3d.textures.GpuTextureView depthTextureView;

    *///?} else {
    @Accessor("colorTextureId")
    public abstract int kineticcore$getColorTextureId();

    @Accessor("depthBufferId")
    public abstract int kineticcore$getDepthBufferId();

    @Accessor("frameBufferId")
    public abstract int kineticcore$getFrameBufferId();

    //?}
    @Unique
    private GpuMemLeakFixHandler.RenderTargetState kineticcore$cleanerState;

    //? if >=26.1 {
    /*@Inject(method = "<init>(Ljava/lang/String;ZZ)V", at = @At("RETURN"))
    private void kineticcore$initCleaner(String label, boolean useDepth, boolean useStencil, CallbackInfo ci) {
    *///?} else {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticcore$initCleaner(boolean useDepth, CallbackInfo ci) {
    //?}
        this.kineticcore$cleanerState = new GpuMemLeakFixHandler.RenderTargetState();
        GpuMemLeakFixHandler.track((RenderTarget) (Object) this, this.kineticcore$cleanerState);
    }

    //? if >=26.1 {
    /*@Inject(method = "createBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnCreate(int width, int height, CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.update(colorTexture, colorTextureView, depthTexture, depthTextureView);
        }
    }
    *///?} else {
    @Inject(method = "createBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnCreate(int width, int height, boolean clearError, CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.update(
                    this.kineticcore$getColorTextureId(),
                    this.kineticcore$getDepthBufferId(),
                    this.kineticcore$getFrameBufferId());
        }
    }
    //?}

    @Inject(method = "destroyBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnDestroy(CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.clear();
        }
    }
}
