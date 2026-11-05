package dev.xyat.kineticcoreitemcreatorvalidation.background.mixin;

import dev.xyat.kineticcoreitemcreatorvalidation.background.BackgroundValidationMode;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** All versions (the window argument is a handle before 26.1 and a Window after, so no handler reads it). Retains MouseHandler's cached coordinates and simulated screen input while leaving the desktop cursor alone. */
@Mixin(InputConstants.class)
public abstract class BackgroundMouseInputMixin {
    @Redirect(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetCursorPos(JDD)V", remap = false))
    private static void kineticValidation$skipNativeWarp(long window, double x, double y) {
        if (BackgroundValidationMode.enabled()) BackgroundValidationMode.mouseWarpSkipped();
        else GLFW.glfwSetCursorPos(window, x, y);
    }

    @Redirect(method = "grabOrReleaseMouse", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetInputMode(JII)V", remap = false))
    private static void kineticValidation$skipNativeCursorMode(long window, int mode, int value) {
        if (BackgroundValidationMode.enabled()) BackgroundValidationMode.cursorModeSkipped();
        else GLFW.glfwSetInputMode(window, mode, value);
    }

    @Inject(method = "updateRawMouseInput", at = @At("HEAD"), cancellable = true)
    private static void kineticValidation$skipNativeRawMode(CallbackInfo ci) {
        if (BackgroundValidationMode.enabled()) ci.cancel();
    }
}
