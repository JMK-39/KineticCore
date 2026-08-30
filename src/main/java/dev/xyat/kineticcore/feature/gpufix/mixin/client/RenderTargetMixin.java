package dev.xyat.kineticcore.feature.gpufix.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.xyat.kineticcore.feature.gpufix.client.GpuMemLeakFixHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @Shadow protected int colorTextureId;
    @Shadow protected int depthBufferId;
    @Shadow public int frameBufferId;

    @Unique
    private GpuMemLeakFixHandler.RenderTargetState kineticcore$cleanerState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticcore$initCleaner(boolean useDepth, CallbackInfo ci) {
        this.kineticcore$cleanerState = new GpuMemLeakFixHandler.RenderTargetState();
        GpuMemLeakFixHandler.track((RenderTarget) (Object) this, this.kineticcore$cleanerState);
    }

    @Inject(method = "createBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnCreate(int width, int height, boolean clearError, CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.update(this.colorTextureId, this.depthBufferId, this.frameBufferId);
        }
    }

    @Inject(method = "destroyBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnDestroy(CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.clear();
        }
    }
}
