package dev.xyat.kineticcore.internal.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 在宿主画布内绘制时使用的 GuiGraphics：裁剪矩形按页面坐标传入，由画布换算为屏幕坐标。
 * GuiGraphics used while drawing inside a host canvas: scissor rectangles are given in page coordinates.
 */
final class CanvasGuiGraphics extends GuiGraphics {
    private final KineticCanvasTransform canvas;

    CanvasGuiGraphics(Minecraft minecraft, GuiGraphics source, KineticCanvasTransform canvas) {
        super(minecraft, source.bufferSource());
        this.canvas = canvas;
    }

    @Override
    public void enableScissor(int left, int top, int right, int bottom) {
        super.enableScissor(
                canvas.toScreenX(left),
                canvas.toScreenY(top),
                canvas.toScreenRight(right),
                canvas.toScreenBottom(bottom)
        );
    }
}
