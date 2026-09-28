package dev.xyat.kineticcore.api.client.gui.render;

import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 仅供 Mixin、原版渲染回调与第三方 API 桥接使用：把原版绘制上下文包装成 {@link KineticGraphics}，以便在原版界面里复用
 * Kinetic 主题、滚动条与绘制工具。页面与控件代码不应使用本类。
 * For mixins, vanilla render callbacks and third-party API bridges only: wraps a vanilla graphics context as {@link KineticGraphics} so
 * Kinetic theme, scrollbar and drawing helpers can be reused inside vanilla screens. Page and control code must
 * not use this class.
 */
public final class KineticGraphicsInterop {
    private KineticGraphicsInterop() {
    }

    /**
     * 取回底层原版绘制上下文，只用于把绘制交给要求 GuiGraphics 的第三方 API（例如 JEI 的 IRecipeLayoutDrawable.draw）。
     * Returns the underlying vanilla graphics context, only for handing drawing to third-party APIs that require a
     * GuiGraphics (for example JEI's IRecipeLayoutDrawable.draw).
     */
    public static GuiGraphics unwrap(KineticGraphics graphics) {
        return GuiGraphicsAdapter.unwrap(graphics);
    }

    /** 包装原版绘制上下文 / Wraps a vanilla graphics context. */
    public static KineticGraphics wrap(GuiGraphics graphics) {
        return GuiGraphicsAdapter.wrap(graphics);
    }
}
