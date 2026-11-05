package dev.xyat.kineticcore.internal.mixin.api.client;

import dev.xyat.kineticcore.internal.client.KineticClientEventRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Offers in-game wheel turns to {@code KineticClientEvents.onMouseScroll} before the game uses them. */
@Mixin(MouseHandler.class)
public abstract class MouseScrollMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void kineticcore$mouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (this.minecraft.screen != null || this.minecraft.player == null || this.minecraft.getOverlay() != null) {
            return;
        }
        if (KineticClientEventRuntime.mouseScrolled(horizontal, vertical)) ci.cancel();
    }
}
