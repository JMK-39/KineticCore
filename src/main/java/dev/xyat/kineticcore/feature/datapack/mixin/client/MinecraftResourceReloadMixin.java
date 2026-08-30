package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.ResourcePackReloadNotifier;
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
    private void kineticcore$datapack$interceptReload(
            CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        if (ResourcePackReloadNotifier.isClosing) {
            ResourcePackReloadNotifier.showTextUntil = System.currentTimeMillis() + 3000L;
            cir.setReturnValue(CompletableFuture.completedFuture(null));
        } else {
            ResourcePackReloadNotifier.showTextUntil = 0L;
        }
    }
}
