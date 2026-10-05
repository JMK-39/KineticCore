package dev.xyat.kineticcore.internal.client.gui.text;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.internal.client.text.KineticTextRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;


/**
 * Text helpers for translated components and width-constrained scrolling rendering.
 */
public final class KineticText {
    private KineticText() {
    }

    /** Creates a translatable component from the supplied language key. */
    public static MutableComponent translatable(String key, Object... args) {
        return KineticI18n.translatable(key, args);
    }

    /** Returns whether the current client language provides the supplied translation key. */
    public static boolean hasTranslation(String key) {
        return KineticTextRuntime.hasTranslation(key);
    }

    /** Resolves one translation to its current-language plain string using Minecraft's standard formatting rules. */
    public static String get(String key, Object... args) {
        return KineticTextRuntime.get(key, args);
    }

    /** Draws a left-aligned string and scrolls it when it exceeds the available width. */
    public static int drawScrollingLeft(
            GuiGraphics graphics,
            Font font,
            String text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        return drawScrollingLeft(graphics, font, Component.literal(text == null ? "" : text), x, y, maxWidth, color, shadow);
    }

    /** Draws a left-aligned component and scrolls it when it exceeds the available width. */
    public static int drawScrollingLeft(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return VanillaGuiDraw.text(graphics, font, text, x, y, color, shadow);
        }
        return drawOverflowing(graphics, font, text, x, y, maxWidth, textWidth, color, shadow);
    }

    /** Draws a centered component and scrolls it when it exceeds the available width. */
    public static int drawScrollingCentered(
            GuiGraphics graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return VanillaGuiDraw.text(graphics, font, text, centerX - textWidth / 2, y, color, shadow);
        }
        return drawOverflowing(graphics, font, text, centerX - maxWidth / 2, y, maxWidth, textWidth, color, shadow);
    }

    /** Draws a right-aligned component and scrolls it when it exceeds the available width. */
    public static int drawScrollingRight(
            GuiGraphics graphics,
            Font font,
            Component text,
            int rightX,
            int y,
            int maxWidth,
            int color,
            boolean shadow
    ) {
        if (graphics == null || font == null || text == null || maxWidth <= 0) return 0;
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            return VanillaGuiDraw.text(graphics, font, text, rightX - textWidth, y, color, shadow);
        }
        return drawOverflowing(graphics, font, text, rightX - maxWidth, y, maxWidth, textWidth, color, shadow);
    }

    /**
     * Text wider than its space never gets cut off: up to twice the space it scrolls back and forth; text longer
     * than that shows its start with an ellipsis and scrolls through in full while the mouse is over it.
     */
    private static int drawOverflowing(GuiGraphics graphics, Font font, Component text, int left, int y, int maxWidth,
                                       int textWidth, int color, boolean shadow) {
        if (textWidth > maxWidth * 2
                && !VanillaGuiDraw.mouseOver(graphics, left, y - 1, maxWidth, font.lineHeight + 2)) {
            return VanillaGuiDraw.text(graphics, font, ellipsized(font, text, maxWidth), left, y, color, shadow);
        }
        KineticRenderRuntime.enableScissor(graphics, left, y - 1, left + maxWidth, y + font.lineHeight + 1);
        try {
            return drawShifted(graphics, font, text, left, y, color, shadow, scrollingOffsetExact(textWidth - maxWidth));
        } finally {
            KineticRenderRuntime.disableScissor(graphics);
        }
    }

    /** The start of the text that fits together with a trailing ellipsis, keeping the text's own styles. */
    private static net.minecraft.util.FormattedCharSequence ellipsized(Font font, Component text, int maxWidth) {
        Component ellipsis = Component.literal("...");
        net.minecraft.network.chat.FormattedText head = font.substrByWidth(text, Math.max(0, maxWidth - font.width(ellipsis)));
        return net.minecraft.locale.Language.getInstance().getVisualOrder(net.minecraft.network.chat.FormattedText.composite(head, ellipsis));
    }

    /** Returns the shared horizontal scrolling offset for custom mixed-content rendering. */
    public static int horizontalScrollOffset(int contentWidth, int viewportWidth) {
        return scrollingOffset(Math.max(0, contentWidth - viewportWidth));
    }

    /** Alias for {@link #horizontalScrollOffset(int, int)} used by custom scrolling renderers. */
    public static int scrollOffset(int contentWidth, int viewportWidth) {
        return horizontalScrollOffset(contentWidth, viewportWidth);
    }

    private static int scrollingOffset(int overflow) {
        return Math.round(scrollingOffsetExact(overflow));
    }

    // Back and forth at a constant 8 GUI pixels per second (twice vanilla's average button speed, at least 0.75
    // seconds per direction), with no easing: the text holds still for half a second at the start and at the end.
    // The offset stays fractional so the text glides instead of stepping a whole pixel at a time.
    private static final double SCROLL_PIXELS_PER_SECOND = 8.0D;
    private static final double MIN_SWEEP_SECONDS = 0.75D;
    private static final double END_PAUSE_SECONDS = 0.5D;

    private static float scrollingOffsetExact(int overflow) {
        if (overflow <= 0) return 0F;
        double sweep = Math.max(overflow / SCROLL_PIXELS_PER_SECOND, MIN_SWEEP_SECONDS);
        double leg = END_PAUSE_SECONDS + sweep;
        double time = (Util.getMillis() / 1000.0D) % (leg * 2.0D);
        double progress;
        if (time < END_PAUSE_SECONDS) {
            progress = 0.0D;                                        // paused at the start
        } else if (time < leg) {
            progress = (time - END_PAUSE_SECONDS) / sweep;          // moving towards the end
        } else if (time < leg + END_PAUSE_SECONDS) {
            progress = 1.0D;                                        // paused at the end
        } else {
            progress = 1.0D - (time - leg - END_PAUSE_SECONDS) / sweep; // moving back
        }
        return (float) (progress * overflow);
    }

    // The scissor is set before the shift, so only the text moves inside the clipped box.
    private static int drawShifted(GuiGraphics graphics, Font font, Component text, int x, int y, int color,
                                   boolean shadow, float offset) {
        VanillaGuiDraw.push(graphics);
        try {
            VanillaGuiDraw.translate(graphics, -offset, 0F, 0F);
            return VanillaGuiDraw.text(graphics, font, text, x, y, color, shadow);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
    }

}
