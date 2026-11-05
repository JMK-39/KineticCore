package dev.xyat.kineticcore.internal.client.gui.theme;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Rounded rectangles for the modern style, drawn row by row in physical screen pixels: the corner pixels get the
 * share of their area the curve covers as alpha, so edges stay smooth at every GUI scale without a shader (the same
 * code runs on every Minecraft version).
 */
public final class RoundedShapes {
    private RoundedShapes() {
    }

    /** Fills a rounded rectangle given in the current local coordinates; the radius is in local units too. */
    public static void fill(GuiGraphics graphics, int x, int y, int width, int height, float radius, int argb) {
        draw(graphics, x, y, width, height, radius, 0F, argb);
    }

    /**
     * Draws the outline of a rounded rectangle, {@code thickness} physical pixels wide (at least one), inside the
     * rectangle's bounds.
     */
    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, float radius, float thickness, int argb) {
        draw(graphics, x, y, width, height, radius, Math.max(1F, thickness), argb);
    }

    /** Physical pixels per local unit at the current pose and GUI scale. */
    public static float pixelScale(GuiGraphics graphics) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) Math.max(0.25D, VanillaGuiDraw.poseScale(graphics) * guiScale);
    }

    private static void draw(GuiGraphics graphics, int x, int y, int width, int height, float radius, float ring, int argb) {
        if (graphics == null || width <= 0 || height <= 0 || (argb >>> 24) == 0) return;
        float scale = pixelScale(graphics);
        int w = Math.max(1, Math.round(width * scale));
        int h = Math.max(1, Math.round(height * scale));
        float r = Math.max(0F, Math.min(radius * scale, Math.min(w, h) / 2F));
        if (r < 0.75F && ring <= 0F) {
            graphics.fill(x, y, x + width, y + height, argb);
            return;
        }
        // One shape is dozens of row fills; they go to the GPU as one batch instead of one draw call each.
        VanillaGuiDraw.batched(graphics, () -> {
            VanillaGuiDraw.push(graphics);
            try {
                VanillaGuiDraw.translate(graphics, x, y, 0);
                VanillaGuiDraw.scale(graphics, 1F / scale, 1F / scale, 1F);
                if (ring <= 0F) solid(graphics, w, h, r, argb);
                else ring(graphics, w, h, r, ring, argb);
            } finally {
                VanillaGuiDraw.pop(graphics);
            }
        });
    }

    private static void solid(GuiGraphics graphics, int w, int h, float r, int argb) {
        int corner = Math.min(h / 2, (int) Math.ceil(r));
        for (int row = 0; row < corner; row++) {
            float inset = inset(row + 0.5F, r);
            span(graphics, row, inset, w - inset, argb);
            span(graphics, h - 1 - row, inset, w - inset, argb);
        }
        if (h - corner * 2 > 0) graphics.fill(0, corner, w, h - corner, argb);
    }

    private static void ring(GuiGraphics graphics, int w, int h, float r, float thickness, int argb) {
        int t = Math.max(1, Math.round(thickness));
        float inner = Math.max(0F, r - t);
        int corner = Math.min(h / 2, Math.max(t, (int) Math.ceil(r)));
        for (int row = 0; row < corner; row++) {
            float center = row + 0.5F;
            float outer = inset(center, r);
            if (row < t) {
                span(graphics, row, outer, w - outer, argb);
                span(graphics, h - 1 - row, outer, w - outer, argb);
                continue;
            }
            float in = t + inset(center - t, inner);
            span(graphics, row, outer, in, argb);
            span(graphics, row, w - in, w - outer, argb);
            span(graphics, h - 1 - row, outer, in, argb);
            span(graphics, h - 1 - row, w - in, w - outer, argb);
        }
        if (h - corner * 2 > 0) {
            graphics.fill(0, corner, t, h - corner, argb);
            graphics.fill(w - t, corner, w, h - corner, argb);
        }
    }

    /** How far the curve of a corner with radius {@code r} stands in from the side, at a row centre {@code y}. */
    private static float inset(float y, float r) {
        if (r <= 0F || y >= r) return 0F;
        float dy = r - y;
        return r - (float) Math.sqrt(Math.max(0F, r * r - dy * dy));
    }

    /** Fills one physical row from {@code a} to {@code b}, with the partly covered end pixels translucent. */
    private static void span(GuiGraphics graphics, int row, float a, float b, int argb) {
        if (b <= a) return;
        int full0 = (int) Math.ceil(a);
        int full1 = (int) Math.floor(b);
        if (full0 > full1) {
            pixel(graphics, (int) Math.floor(a), row, b - a, argb);
            return;
        }
        if (a < full0) pixel(graphics, full0 - 1, row, full0 - a, argb);
        if (full1 > full0) graphics.fill(full0, row, full1, row + 1, argb);
        if (b > full1) pixel(graphics, full1, row, b - full1, argb);
    }

    private static void pixel(GuiGraphics graphics, int x, int row, float coverage, int argb) {
        int alpha = Math.round((argb >>> 24) * Math.max(0F, Math.min(1F, coverage)));
        if (alpha <= 0) return;
        graphics.fill(x, row, x + 1, row + 1, (alpha << 24) | (argb & 0x00FFFFFF));
    }
}
