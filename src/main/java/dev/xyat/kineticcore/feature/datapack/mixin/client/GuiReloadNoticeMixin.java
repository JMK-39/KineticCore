package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.ResourcePackReloadNotifier;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiReloadNoticeMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void kineticcore$datapack$renderReloadNotice(
            GuiGraphics guiGraphics,
            float partialTick,
            CallbackInfo ci
    ) {
        ResourcePackReloadNotifier.render(
                guiGraphics,
                guiGraphics.guiWidth(),
                guiGraphics.guiHeight()
        );
    }
}
