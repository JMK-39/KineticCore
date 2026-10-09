package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;

@Mixin(Resource.class)
public abstract class ResourceReadContextMixin {
    @Inject(method = "openAsReader", at = @At("RETURN"), cancellable = true)
    private void kineticcore$context(CallbackInfoReturnable<BufferedReader> callback) {
        callback.setReturnValue(DatapackReferences.reader((Resource) (Object) this, callback.getReturnValue()));
    }
}
