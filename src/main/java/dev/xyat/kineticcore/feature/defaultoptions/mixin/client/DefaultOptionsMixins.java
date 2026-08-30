package dev.xyat.kineticcore.feature.defaultoptions.mixin.client;

import dev.xyat.kineticcore.feature.defaultoptions.OptionsManager;
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
            OptionsManager.enforceDefaultOptions();
            OptionsManager.applyCustomKeyDefaults((Options) (Object) this);
        }

        @Inject(method = "load(Z)V", at = @At("HEAD"), remap = false, require = 0)
        private void kineticcore$preLoadForge(boolean limited, CallbackInfo ci) {
            OptionsManager.enforceDefaultOptions();
            OptionsManager.applyCustomKeyDefaults((Options) (Object) this);
        }
    }
}