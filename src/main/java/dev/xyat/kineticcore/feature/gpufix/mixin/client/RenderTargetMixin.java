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

    // 注入一个专属的独立状态对象
    @Unique
    private GpuMemLeakFixHandler.RenderTargetState kineticcore$cleanerState;

    // 1. 在构造函数完毕时，初始化状态并注册到 Cleaner
    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticcore$initCleaner(boolean useDepth, CallbackInfo ci) {
        this.kineticcore$cleanerState = new GpuMemLeakFixHandler.RenderTargetState();
        GpuMemLeakFixHandler.track((RenderTarget) (Object) this, this.kineticcore$cleanerState);
    }

    // 2. 每当 RenderTarget 创建/重建缓冲区时，同步最新的 ID 到状态对象中
    @Inject(method = "createBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnCreate(int width, int height, boolean clearError, CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.colorTextureId = this.colorTextureId;
            this.kineticcore$cleanerState.depthBufferId = this.depthBufferId;
            this.kineticcore$cleanerState.frameBufferId = this.frameBufferId;
        }
    }

    // 3. 当游戏原生逻辑正常销毁缓冲区时，同步状态。
    // （此时这三个 Shadow 字段都会被原版游戏设为 -1，所以状态也会变成 -1。
    // 等后续触发 GC 时，Cleaner 检测到是 -1，就不会误加进清理队列了，完美闭环！）
    @Inject(method = "destroyBuffers", at = @At("TAIL"))
    private void kineticcore$syncStateOnDestroy(CallbackInfo ci) {
        if (this.kineticcore$cleanerState != null) {
            this.kineticcore$cleanerState.colorTextureId = this.colorTextureId;
            this.kineticcore$cleanerState.depthBufferId = this.depthBufferId;
            this.kineticcore$cleanerState.frameBufferId = this.frameBufferId;
        }
    }
}