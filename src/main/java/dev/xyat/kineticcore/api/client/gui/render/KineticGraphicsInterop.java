package dev.xyat.kineticcore.api.client.gui.render;

import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 仅供 Mixin 与原版渲染回调使用：把原版绘制上下文包装成 {@link KineticGraphics}，以便在原版界面里复用
 * Kinetic 主题、滚动条与绘制工具。页面与控件代码不应使用本类。
 * For mixins and vanilla render callbacks only: wraps a vanilla graphics context as {@link KineticGraphics} so
 * Kinetic theme, scrollbar and drawing helpers can be reused inside vanilla screens. Page and control code must
 * not use this class.
 */
public final class KineticGraphicsInterop {
    private KineticGraphicsInterop() {
    }

    /** 包装原版绘制上下文 / Wraps a vanilla graphics context. */
    public static KineticGraphics wrap(GuiGraphics graphics) {
        return GuiGraphicsAdapter.wrap(graphics);
    }
}
