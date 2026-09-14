package dev.xyat.kineticcore.internal.mixin.datapack.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public abstract class MinecraftResourceReloadMixin {

    @Inject(
            method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void kineticcore$datapack$interceptReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (KineticClientHookRuntime.interceptResourceReloadStart()) {
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        }
    }
}
