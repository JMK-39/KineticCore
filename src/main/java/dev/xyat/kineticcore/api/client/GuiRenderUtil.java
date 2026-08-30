package dev.xyat.kineticcore.api.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class GuiRenderUtil {

    public static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int bgColor, int outlineColor) {
        g.fill(x, y, x + w, y + h, bgColor);
        g.renderOutline(x, y, w, h, outlineColor);
    }

    public static void drawStandardPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, 0xFF1C1C1C, 0xFF666666);
    }

    public static void drawDarkPanel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, 0x88000000, 0xFF555555);
    }

    public static void drawShadowOverlay(GuiGraphics g, int screenWidth, int screenHeight) {
        g.fill(0, 0, screenWidth, screenHeight, 0xC0000000);
    }

    public static void drawCheckerboard(GuiGraphics g, int x, int y, int w, int h, int cellSize, int colorA, int colorB) {
        int size = Math.max(1, cellSize);
        for (int yy = 0; yy < h; yy += size) {
            int drawH = Math.min(size, h - yy);
            for (int xx = 0; xx < w; xx += size) {
                int drawW = Math.min(size, w - xx);
                boolean useA = ((xx / size) + (yy / size)) % 2 == 0;
                g.fill(x + xx, y + yy, x + xx + drawW, y + yy + drawH, useA ? colorA : colorB);
            }
        }
    }

    public static String trimText(Font font, String text, int width) {
        if (text == null) return "";
        return font.width(text) > width ? font.plainSubstrByWidth(text, Math.max(8, width - font.width("..."))) + "..." : text;
    }

    public static boolean isHovering(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}