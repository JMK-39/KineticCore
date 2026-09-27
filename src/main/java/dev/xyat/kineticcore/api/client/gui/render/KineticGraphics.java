package dev.xyat.kineticcore.api.client.gui.render;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

/**
 * Kinetic 页面、控件、HUD 与主题绘制唯一使用的 2D 绘制表面。坐标就是调用方所在的页面坐标系，缩放与裁剪换算由
 * KineticCore 负责。它刻意只提供 2D 操作（平移、缩放、旋转、层级抬升），以便在原版 GUI 渲染管线变化时由核心内部
 * 适配，附属代码不需要改动。
 * The only 2D drawing surface used by Kinetic pages, controls, HUD layers and the theme. Coordinates are the caller's
 * page coordinates; scaling and clip conversion are owned by KineticCore. It deliberately exposes only 2D
 * operations (translate, scale, rotate, layer raise) so KineticCore can adapt to vanilla GUI pipeline changes
 * without addon changes.
 */
public interface KineticGraphics {
    /** 填充矩形（左上与右下角，右下不含）/ Fills a rectangle from (x1, y1) inclusive to (x2, y2) exclusive. */
    void fill(int x1, int y1, int x2, int y2, int argb);

    /** 竖直渐变填充 / Fills a vertical gradient. */
    void fillGradient(int x1, int y1, int x2, int y2, int topArgb, int bottomArgb);

    /** 1 像素描边 / Draws a 1-pixel outline. */
    void outline(int x, int y, int width, int height, int argb);

    /** 绘制文本，返回结束 X / Draws text and returns the end X. */
    int text(String text, int x, int y, int argb, boolean shadow);

    /** 绘制文本组件，返回结束 X / Draws a text component and returns the end X. */
    int text(Component text, int x, int y, int argb, boolean shadow);

    /** 绘制已排版文本，返回结束 X / Draws formatted text and returns the end X. */
    int text(FormattedCharSequence text, int x, int y, int argb, boolean shadow);

    /** 无阴影文本 / Text without shadow. */
    default int text(Component text, int x, int y, int argb) {
        return text(text, x, y, argb, false);
    }

    /** 无阴影文本 / Text without shadow. */
    default int text(String text, int x, int y, int argb) {
        return text(text, x, y, argb, false);
    }

    /** 以 centerX 居中绘制 / Draws text centered on centerX. */
    void centeredText(Component text, int centerX, int y, int argb, boolean shadow);

    /** 以 centerX 居中绘制 / Draws text centered on centerX. */
    void centeredText(String text, int centerX, int y, int argb, boolean shadow);

    /** 自动换行绘制 / Draws word-wrapped text. */
    void wrappedText(Component text, int x, int y, int maxWidth, int argb);

    /** 左对齐绘制，超宽时来回滚动，返回实际宽度 / Left-aligned text that scrolls when too wide; returns drawn width. */
    int scrollingText(Component text, int x, int y, int maxWidth, int argb, boolean shadow);

    /** 居中绘制，超宽时滚动 / Centered text that scrolls when too wide. */
    int scrollingTextCentered(Component text, int centerX, int y, int maxWidth, int argb, boolean shadow);

    /** 右对齐绘制，超宽时滚动 / Right-aligned text that scrolls when too wide. */
    int scrollingTextRight(Component text, int rightX, int y, int maxWidth, int argb, boolean shadow);

    /** 绘制物品图标 / Draws an item icon. */
    void item(ItemStack stack, int x, int y);

    /** 以指定透明度绘制物品图标 / Draws an item icon with the requested opacity. */
    void item(ItemStack stack, int x, int y, float alpha);

    /** 绘制无实体光照的物品图标 / Draws an item icon without entity-dependent effects. */
    void fakeItem(ItemStack stack, int x, int y);

    /** 绘制物品数量与耐久条 / Draws item count and durability decorations. */
    void itemDecorations(ItemStack stack, int x, int y);

    /** 用自定义数量文字绘制物品装饰 / Draws item decorations with a custom count text. */
    void itemDecorations(ItemStack stack, int x, int y, String countText);

    /** 绘制贴图区域（1:1）/ Draws a texture region at 1:1 scale. */
    void texture(KineticTexture texture, int x, int y, int u, int v, int width, int height);

    /** 把贴图区域缩放绘制到目标尺寸 / Draws a texture region scaled to the target size. */
    void texture(KineticTexture texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight);

    /** 以 ARGB 颜色染色绘制贴图区域（1:1）/ Draws a texture region at 1:1 scale tinted with an ARGB color. */
    void texture(KineticTexture texture, int x, int y, int u, int v, int width, int height, int argb);

    /** 绘制状态效果图标 / Draws a mob effect icon. */
    void effectIcon(MobEffect effect, int x, int y, int size);

    /** 水平线（含两端）/ Horizontal line, both ends inclusive. */
    default void hLine(int x1, int x2, int y, int argb) {
        fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, argb);
    }

    /** 垂直线（含两端）/ Vertical line, both ends inclusive. */
    default void vLine(int x, int y1, int y2, int argb) {
        fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, argb);
    }

    /** 保存变换状态 / Saves the transform state. */
    void push();

    /** 恢复变换状态 / Restores the transform state. */
    void pop();

    /** 平移 / Translates. */
    void translate(float x, float y);

    /** 缩放 / Scales. */
    void scale(float x, float y);

    /** 绕当前原点顺时针旋转（角度）/ Rotates clockwise around the current origin, in degrees. */
    void rotate(float degrees);

    /**
     * 让之后绘制的内容显示在已绘制内容（包括物品图标）之上，直到对应 {@link #pop()}。
     * Makes subsequent drawing appear above everything drawn so far (item icons included) until the matching
     * {@link #pop()}.
     */
    void raise(int steps);

    /** 开始裁剪（页面坐标，可嵌套）/ Starts clipping in page coordinates; clips nest. */
    void scissor(int left, int top, int right, int bottom);

    /** 结束最近一次裁剪 / Ends the most recent clip. */
    void endScissor();

    /** 在裁剪区域内执行绘制 / Runs drawing inside a clip rectangle. */
    default void clipped(int left, int top, int right, int bottom, Runnable drawing) {
        scissor(left, top, right, bottom);
        try {
            drawing.run();
        } finally {
            endScissor();
        }
    }

    /** 在 push/pop 之间执行绘制 / Runs drawing between push and pop. */
    default void isolated(Runnable drawing) {
        push();
        try {
            drawing.run();
        } finally {
            pop();
        }
    }

    /** 文本宽度 / Text width. */
    int textWidth(String text);

    /** 文本宽度 / Text width. */
    int textWidth(Component text);

    /** 文本宽度 / Text width. */
    int textWidth(FormattedCharSequence text);

    /** 行高 / Line height. */
    int lineHeight();
}
