package dev.xyat.kineticcore.internal.mixin.api.datapack.client;

import dev.xyat.kineticcore.internal.client.KineticClientHookRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenReloadNoticeMixin {
    @Accessor("width")
    public abstract int kineticcore$getWidth();

    @Accessor("height")
    public abstract int kineticcore$getHeight();

    @Inject(method = "render", at = @At("TAIL"))
    private void kineticcore$datapack$renderReloadNotice(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        KineticClientHookRuntime.renderResourceReloadUi(
                guiGraphics, this.kineticcore$getWidth(), this.kineticcore$getHeight());
    }
}
