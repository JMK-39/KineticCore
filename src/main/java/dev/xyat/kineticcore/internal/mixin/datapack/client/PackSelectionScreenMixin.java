package dev.xyat.kineticcore.internal.mixin.datapack.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin {

    @Inject(method = "onClose", at = @At("HEAD"))
    private void kineticcore$datapack$onCloseStart(CallbackInfo ci) {
        KineticClientHookRuntime.setPackScreenClosing(true);
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    private void kineticcore$datapack$onCloseEnd(CallbackInfo ci) {
        KineticClientHookRuntime.setPackScreenClosing(false);
    }
}
