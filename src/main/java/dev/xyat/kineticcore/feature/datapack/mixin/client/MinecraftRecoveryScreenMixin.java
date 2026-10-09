package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.recovery.client.DatapackRecoveryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRecoveryScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void kineticcore$replaceFailure(Screen screen, CallbackInfo callback) {
        if (DatapackRecoveryClient.replaceFailure(screen)) callback.cancel();
    }
}
