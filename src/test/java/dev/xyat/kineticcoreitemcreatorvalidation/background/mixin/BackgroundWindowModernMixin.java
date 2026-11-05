package dev.xyat.kineticcoreitemcreatorvalidation.background.mixin;

//? if >=26.1 {
/*import dev.xyat.kineticcoreitemcreatorvalidation.background.BackgroundValidationMode;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26.1 test window policy: the same as BackgroundWindowMixin, but the GPU backend sets the context hints in
// createGlfwWindow, the handle field is named handle, and the per-frame hook is updateFullscreenIfChanged. The early
// loading window must be disabled (fml.toml earlyWindowControl=false), or createGlfwWindow takes over that window.
@Mixin(Window.class)
public abstract class BackgroundWindowModernMixin {
    @Shadow @Final private long handle;
    @Shadow private boolean fullscreen;
    @Shadow private boolean actuallyFullscreen;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static DisplayData kineticValidation$windowedDisplay(DisplayData display) {
        if (!BackgroundValidationMode.enabled()) return display;
        return new DisplayData(BackgroundValidationMode.width(), BackgroundValidationMode.height(),
                display.fullscreenWidth(), display.fullscreenHeight(), false);
    }

    // Added after the backend's own hints and right before creation, so they cannot be reset.
    @Inject(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private static void kineticValidation$unfocusedWindowHints(CallbackInfoReturnable<Long> cir) {
        if (!BackgroundValidationMode.enabled()) return;
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE);
        if (BackgroundValidationMode.hidden()) GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticValidation$reportWindow(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        if (BackgroundValidationMode.hidden()) GLFW.glfwHideWindow(handle);
        System.out.println("KINETIC_BACKGROUND_VALIDATION windowPolicy=true focused="
                + GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED)
                + " visible=" + GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE)
                + " requested=" + BackgroundValidationMode.width() + "x" + BackgroundValidationMode.height());
    }

    @Inject(method = "updateFullscreenIfChanged", at = @At("HEAD"))
    private void kineticValidation$keepHidden(CallbackInfo ci) {
        if (BackgroundValidationMode.enabled() && BackgroundValidationMode.hidden()
                && GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE) {
            GLFW.glfwHideWindow(handle);
        }
    }

    @Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
    private void kineticValidation$stayWindowed(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        fullscreen = actuallyFullscreen = false;
        width = windowedWidth;
        height = windowedHeight;
        int[] currentWidth = new int[1], currentHeight = new int[1];
        GLFW.glfwGetWindowSize(handle, currentWidth, currentHeight);
        if (currentWidth[0] != width || currentHeight[0] != height) GLFW.glfwSetWindowSize(handle, width, height);
        ci.cancel();
    }

    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
    private void kineticValidation$skipFullscreen(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        fullscreen = actuallyFullscreen = false;
        ci.cancel();
    }
}
*///?}
