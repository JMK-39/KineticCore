package dev.xyat.kineticcore.api.client.gui.text;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * GUI 文本测量与排版（宽度、截断、换行）。翻译请使用 {@code KineticI18n}，绘制请使用 {@code KineticGraphics}。
 * GUI text measurement and layout (width, trimming, wrapping). Use {@code KineticI18n} for translations and
 * {@code KineticGraphics} for drawing.
 */
public final class KineticText {
    private static final String ELLIPSIS = "...";

    private KineticText() {
    }

    private static Font font() {
        return KineticClientRuntimeImpl.font();
    }

    /** 文本像素宽度 / Pixel width of text. */
    public static int width(String text) {
        return font().width(text == null ? "" : text);
    }

    /** 组件像素宽度 / Pixel width of a component. */
    public static int width(Component text) {
        return font().width(text == null ? Component.empty() : text);
    }

    /** 已排版文本像素宽度 / Pixel width of formatted text. */
    public static int width(FormattedCharSequence text) {
        return font().width(text == null ? FormattedCharSequence.EMPTY : text);
    }

    /** 行高 / Line height. */
    public static int lineHeight() {
        return font().lineHeight;
    }

    /** 截断到指定宽度（不加省略号）/ Trims text to a width without an ellipsis. */
    public static String trim(String text, int maxWidth) {
        return font().plainSubstrByWidth(text == null ? "" : text, Math.max(0, maxWidth));
    }

    /** 超宽时截断并追加省略号 / Trims text and appends an ellipsis when it overflows. */
    public static String ellipsize(String text, int maxWidth) {
        String safe = text == null ? "" : text;
        if (width(safe) <= maxWidth) return safe;
        int available = Math.max(0, maxWidth - width(ELLIPSIS));
        return trim(safe, available) + ELLIPSIS;
    }

    /** 按宽度换行 / Wraps a component into lines of at most maxWidth. */
    public static List<FormattedCharSequence> wrap(Component text, int maxWidth) {
        return font().split(text == null ? FormattedText.EMPTY : text, Math.max(1, maxWidth));
    }

    /** 换行后的总高度 / Total height after wrapping. */
    public static int wrappedHeight(Component text, int maxWidth) {
        return wrap(text, maxWidth).size() * lineHeight();
    }

    /**
     * 自绘混合内容（文字 + 图标）的往返滚动偏移像素；内容不超宽时为 0。与 {@code scrollingText} 使用同一节奏。
     * Ping-pong scroll offset in pixels for self-drawn mixed content (text + icons); 0 when the content fits.
     * Uses the same timing as {@code scrollingText}.
     */
    public static int scrollOffset(int contentWidth, int viewportWidth) {
        return dev.xyat.kineticcore.internal.client.gui.text.KineticText.scrollOffset(contentWidth, viewportWidth);
    }
}
