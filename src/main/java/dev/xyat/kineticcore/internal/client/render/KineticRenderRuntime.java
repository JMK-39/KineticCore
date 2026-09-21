package dev.xyat.kineticcore.internal.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

/** Internal owner for low-level render-state operations used by the public GUI API. */
public final class KineticRenderRuntime {
    private KineticRenderRuntime() {
    }

    /** Enables alpha blending. */
    public static void enableBlend() {
        RenderSystem.enableBlend();
    }

    /** Disables alpha blending. */
    public static void disableBlend() {
        RenderSystem.disableBlend();
    }

    /** Enables depth testing. */
    public static void enableDepthTest() {
        RenderSystem.enableDepthTest();
    }

    /** Disables depth testing. */
    public static void disableDepthTest() {
        RenderSystem.disableDepthTest();
    }

    /** Runs one action with depth testing disabled and restores the standard enabled state afterward. */
    public static void runWithoutDepthTest(Runnable action) {
        Objects.requireNonNull(action, "action");
        RenderSystem.disableDepthTest();
        try {
            action.run();
        } finally {
            RenderSystem.enableDepthTest();
        }
    }

    /** Enables one GUI scissor rectangle using screen-space coordinates. */
    public static void enableScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        Objects.requireNonNull(graphics, "graphics").enableScissor(left, top, right, bottom);
    }

    /** Disables the active GUI scissor rectangle. */
    public static void disableScissor(GuiGraphics graphics) {
        Objects.requireNonNull(graphics, "graphics").disableScissor();
    }

    /** Restores the standard opaque-white shader color. */
    public static void resetShaderColor() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
