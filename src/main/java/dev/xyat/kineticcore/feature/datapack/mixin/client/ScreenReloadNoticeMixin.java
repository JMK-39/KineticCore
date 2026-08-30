package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.ResourcePackReloadNotifier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenReloadNoticeMixin {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "render", at = @At("TAIL"))
    private void kineticcore$datapack$renderReloadNotice(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        ResourcePackReloadNotifier.render(guiGraphics, this.width, this.height);
    }
}
