package dev.xyat.kineticcore.internal.mixin.api.datapack.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiReloadNoticeMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void kineticcore$datapack$renderReloadNotice(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        KineticClientHookRuntime.renderResourceReloadUi(
                guiGraphics,
                guiGraphics.guiWidth(),
                guiGraphics.guiHeight()
        );
    }
}
