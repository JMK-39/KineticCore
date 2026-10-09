package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences;
import net.minecraft.server.WorldLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(WorldLoader.class)
public abstract class ReferenceLifecycleMixin {
    @Inject(method = "load", at = @At("RETURN"))
    private static void kineticcore$finish(CallbackInfoReturnable<CompletableFuture<?>> callback) {
        long attempt = DatapackReferences.generation();
        callback.getReturnValue().whenComplete((result, error) -> DatapackReferences.finish(attempt));
    }
}
