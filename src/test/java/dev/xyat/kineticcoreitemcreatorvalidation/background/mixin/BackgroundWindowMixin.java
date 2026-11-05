package dev.xyat.kineticcoreitemcreatorvalidation.background.mixin;

//? if <26.1 {
import dev.xyat.kineticcoreitemcreatorvalidation.background.BackgroundValidationMode;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge 1.20.1 and NeoForge 1.21.1 test window policy (same fields and hints); 26.1 is BackgroundWindowModernMixin. The
 * loader's early loading window must be disabled (fml.toml earlyWindowControl=false): it opens before any mixin. */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    @Shadow @Final private long window;
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
                display.fullscreenWidth, display.fullscreenHeight, false);
    }

    // Vanilla's context hints follow defaults. Observe their arguments instead of replacing calls owned by
    // Oculus (default hints) or Embeddium (window creation). These context hints never change focus/visibility.
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false), index = 1)
    private int kineticValidation$unfocusedWindowHints(int value) {
        if (BackgroundValidationMode.enabled()) {
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, GLFW.GLFW_FALSE);
            if (BackgroundValidationMode.hidden()) GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        }
        return value;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticValidation$reportWindow(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        if (BackgroundValidationMode.hidden()) GLFW.glfwHideWindow(window);
        System.out.println("KINETIC_BACKGROUND_VALIDATION windowPolicy=true focused="
                + GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED)
                + " visible=" + GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE)
                + " requested=" + BackgroundValidationMode.width() + "x" + BackgroundValidationMode.height());
    }

    @Inject(method = "updateDisplay", at = @At("HEAD"))
    private void kineticValidation$keepHidden(CallbackInfo ci) {
        if (BackgroundValidationMode.enabled() && BackgroundValidationMode.hidden()
                && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) != GLFW.GLFW_FALSE) {
            GLFW.glfwHideWindow(window);
        }
    }

    @Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
    private void kineticValidation$stayWindowed(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        fullscreen = actuallyFullscreen = false;
        width = windowedWidth;
        height = windowedHeight;
        int[] currentWidth = new int[1], currentHeight = new int[1];
        GLFW.glfwGetWindowSize(window, currentWidth, currentHeight);
        if (currentWidth[0] != width || currentHeight[0] != height) GLFW.glfwSetWindowSize(window, width, height);
        // Skip the monitor switch, including Chloride's redirect of it, for this test process only.
        ci.cancel();
    }

    @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
    private void kineticValidation$skipFullscreen(CallbackInfo ci) {
        if (!BackgroundValidationMode.enabled()) return;
        fullscreen = actuallyFullscreen = false;
        ci.cancel();
    }

}
//?}
