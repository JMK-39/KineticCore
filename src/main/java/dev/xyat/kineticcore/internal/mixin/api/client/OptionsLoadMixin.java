package dev.xyat.kineticcore.internal.mixin.api.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class OptionsLoadMixin {
    @Mixin(Options.class)
    public static class FileHandler {
        @Inject(method = "load()V", at = @At("HEAD"))
        private void kineticcore$preLoadVanilla(CallbackInfo ci) {
            KineticClientHookRuntime.fireOptionsLoading((Options) (Object) this);
        }

        @Inject(method = "load(Z)V", at = @At("HEAD"), remap = false, require = 0)
        private void kineticcore$preLoadForge(boolean limited, CallbackInfo ci) {
            KineticClientHookRuntime.fireOptionsLoading((Options) (Object) this);
        }
    }
}
