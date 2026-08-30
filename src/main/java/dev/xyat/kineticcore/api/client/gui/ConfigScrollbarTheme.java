package dev.xyat.kineticcore.api.client.gui;

import dev.xyat.kineticcore.api.client.ScrollUtil;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Opt-in scrollbar colors for kineticcore configuration screens.
 *
 * <p>This deliberately leaves the shared scrollbar/controller defaults
 * untouched so screens supplied by dependent mods keep their existing look.</p>
 */
public final class ConfigScrollbarTheme {
    public static final int BORDER_COLOR = 0xFF66522A;
    public static final int TRACK_COLOR = 0xFF211B10;
    public static final int THUMB_COLOR = ScrollUtil.DEFAULT_THUMB_COLOR;
    public static final int THUMB_HOVER_COLOR = ScrollUtil.DEFAULT_THUMB_HOVER_COLOR;

    private ConfigScrollbarTheme() {
    }

    public static void render(
            GridScrollController controller,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            int minThumbHeight
    ) {
        controller.renderFramed(
                graphics,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                minThumbHeight,
                BORDER_COLOR,
                TRACK_COLOR,
                THUMB_COLOR,
                THUMB_HOVER_COLOR
        );
    }

    public static void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbHeight,
            int maxScroll,
            int currentScroll,
            boolean dragging
    ) {
        if (maxScroll <= 0) return;

        int safeThumbHeight = Math.max(1, Math.min(height, thumbHeight));
        int safeScroll = Math.max(0, Math.min(maxScroll, currentScroll));
        int thumbY = y + Math.round(
                (height - safeThumbHeight) * (safeScroll / (float) maxScroll)
        );
        boolean hovered = ScrollUtil.isHoveringThumb(
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                safeThumbHeight,
                maxScroll,
                safeScroll
        );

        graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
        if (width > 2 && height > 2) {
            graphics.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    TRACK_COLOR
            );
        }
        graphics.fill(
                x,
                thumbY,
                x + width,
                thumbY + safeThumbHeight,
                dragging || hovered ? THUMB_HOVER_COLOR : THUMB_COLOR
        );
    }
}
