package dev.xyat.kineticcore.internal.mixin.defaultoptions.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class DefaultOptionsMixins {
    @Mixin(Options.class)
    public static class FileHandler {
        @Inject(method = "load()V", at = @At("HEAD"))
        private void kineticcore$preLoadVanilla(CallbackInfo ci) {
            Options options = (Options) (Object) this;
            KineticClientHookRuntime.fireOptionsLoading(options);
        }

        @Inject(method = "load(Z)V", at = @At("HEAD"), remap = false, require = 0)
        private void kineticcore$preLoadForge(boolean limited, CallbackInfo ci) {
            Options options = (Options) (Object) this;
            KineticClientHookRuntime.fireOptionsLoading(options);
        }
    }
}
