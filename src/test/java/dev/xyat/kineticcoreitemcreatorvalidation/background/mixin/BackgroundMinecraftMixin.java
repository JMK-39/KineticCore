package dev.xyat.kineticcoreitemcreatorvalidation.background.mixin;

import dev.xyat.kineticcoreitemcreatorvalidation.background.BackgroundValidationMode;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** All versions: keeps the hidden test client ticking without requesting native focus or changing saved pause settings. */
@Mixin(Minecraft.class)
public abstract class BackgroundMinecraftMixin {
    @Inject(method = "isWindowActive", at = @At("HEAD"), cancellable = true)
    private void kineticValidation$logicalActivity(CallbackInfoReturnable<Boolean> cir) {
        if (BackgroundValidationMode.enabled()) cir.setReturnValue(true);
    }
}
