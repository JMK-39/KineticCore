package dev.xyat.kineticcore.api.client.text;

import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.internal.client.text.KineticTextRuntime;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
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
            return graphics.drawString(font, text, x, y, color, shadow);
        }
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        KineticRenderRuntime.enableScissor(graphics, x, y - 1, x + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, x - offset, y, color, shadow);
        } finally {
            KineticRenderRuntime.disableScissor(graphics);
        }
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
            return graphics.drawString(font, text, centerX - textWidth / 2, y, color, shadow);
        }
        int left = centerX - maxWidth / 2;
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        KineticRenderRuntime.enableScissor(graphics, left, y - 1, left + maxWidth, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, left - offset, y, color, shadow);
        } finally {
            KineticRenderRuntime.disableScissor(graphics);
        }
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
            return graphics.drawString(font, text, rightX - textWidth, y, color, shadow);
        }
        int left = rightX - maxWidth;
        int overflow = textWidth - maxWidth;
        int offset = scrollingOffset(overflow);
        KineticRenderRuntime.enableScissor(graphics, left, y - 1, rightX, y + font.lineHeight + 1);
        try {
            return graphics.drawString(font, text, left - offset, y, color, shadow);
        } finally {
            KineticRenderRuntime.disableScissor(graphics);
        }
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
        if (overflow <= 0) return 0;
        double seconds = Util.getMillis() / 1000.0D;
        double duration = Math.max(overflow * 0.5D, 3.0D);
        double phase = Math.sin((Math.PI / 2.0D) * Math.cos((Math.PI * 2.0D) * seconds / duration)) / 2.0D + 0.5D;
        return (int) Math.round(Mth.lerp(phase, 0.0D, (double) overflow));
    }

}
