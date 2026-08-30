package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.ResourcePackReloadNotifier;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin {

    @Inject(method = "onClose", at = @At("HEAD"))
    private void kineticcore$datapack$onCloseStart(CallbackInfo ci) {
        ResourcePackReloadNotifier.isClosing = true;
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    private void kineticcore$datapack$onCloseEnd(CallbackInfo ci) {
        ResourcePackReloadNotifier.isClosing = false;
    }
}
