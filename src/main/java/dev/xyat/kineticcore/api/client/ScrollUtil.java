package dev.xyat.kineticcore.api.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class ScrollUtil {
    public static final int DEFAULT_TRACK_COLOR = 0xFF171717;
    public static final int DEFAULT_THUMB_COLOR = 0xFFFF9800;
    public static final int DEFAULT_THUMB_HOVER_COLOR = 0xFFFFD700;

    /**
     * 计算滑块高度
     */
    public static int calculateThumbHeight(int trackHeight, int visibleItems, int totalItems, int minHeight) {
        if (totalItems <= 0) return minHeight;
        return Math.max(minHeight, (int) ((float) visibleItems / totalItems * trackHeight));
    }

    /**
     * 根据鼠标位置计算滚动偏移量
     */
    public static int calculateScrollOffset(double mouseY, int trackY, int trackHeight, int thumbHeight, int maxScroll) {
        double relativeY = mouseY - trackY - (thumbHeight / 2.0);
        double scrollableHeight = trackHeight - thumbHeight;

        if (scrollableHeight > 0) {
            int offset = (int) Math.round((relativeY / scrollableHeight) * maxScroll);
            return Mth.clamp(offset, 0, maxScroll);
        }
        return 0;
    }

    /**
     * Renders the default scrollbar theme.
     */
    public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll, boolean isDragging) {
        renderScrollbar(g, barX, barY, barWidth, trackHeight, thumbHeight, maxScroll, currentScroll, isDragging, DEFAULT_TRACK_COLOR, DEFAULT_THUMB_COLOR, DEFAULT_THUMB_HOVER_COLOR);
    }

    /**
     * Renders the default scrollbar theme with automatic thumb hover highlighting.
     */
    public static void renderScrollbar(
            GuiGraphics g,
            double mouseX,
            double mouseY,
            int barX,
            int barY,
            int barWidth,
            int trackHeight,
            int thumbHeight,
            int maxScroll,
            int currentScroll,
            boolean isDragging
    ) {
        int safeMaxScroll = Math.max(0, maxScroll);
        if (safeMaxScroll <= 0 || trackHeight <= 0 || barWidth <= 0) return;
        int safeThumbHeight = Mth.clamp(thumbHeight, 1, trackHeight);
        int safeCurrentScroll = Mth.clamp(currentScroll, 0, safeMaxScroll);
        boolean hovered = isHoveringThumb(
                mouseX,
                mouseY,
                barX,
                barY,
                barWidth,
                trackHeight,
                safeThumbHeight,
                safeMaxScroll,
                safeCurrentScroll
        );
        renderScrollbar(
                g,
                barX,
                barY,
                barWidth,
                trackHeight,
                safeThumbHeight,
                safeMaxScroll,
                safeCurrentScroll,
                isDragging || hovered,
                DEFAULT_TRACK_COLOR,
                DEFAULT_THUMB_COLOR,
                DEFAULT_THUMB_HOVER_COLOR
        );
    }

    /**
     * 渲染可自定义颜色的滚动条。
     */
    public static void renderScrollbar(GuiGraphics g, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll, boolean highlighted, int trackColor, int thumbColor, int highlightColor) {
        if (maxScroll <= 0) return;
        int thumbY = barY + (int) ((float) currentScroll / maxScroll * (trackHeight - thumbHeight));
        g.fill(barX, barY, barX + barWidth, barY + trackHeight, trackColor);
        g.fill(barX, thumbY, barX + barWidth, thumbY + thumbHeight, highlighted ? highlightColor : thumbColor);
    }

    /**
     * 鼠标是否悬浮在滚动条滑块上。
     */
    public static boolean isHoveringThumb(double mouseX, double mouseY, int barX, int barY, int barWidth, int trackHeight, int thumbHeight, int maxScroll, int currentScroll) {
        if (maxScroll <= 0) return false;
        int thumbY = barY + (int) ((float) currentScroll / maxScroll * (trackHeight - thumbHeight));
        return mouseX >= barX && mouseX <= barX + barWidth && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
    }
}
