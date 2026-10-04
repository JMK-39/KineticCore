package dev.xyat.kineticcore.internal.client.gui.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vanilla GUI drawing calls whose names or behaviour differ between Minecraft versions. 26.1 renamed GuiGraphics
 * to GuiGraphicsExtractor, drawString to text, the pose stack became a 2D matrix stack, and tooltips are queued for
 * the end of the frame. Internal code draws through these methods so it reads the same on every version.
 */
public final class VanillaGuiDraw {
    private VanillaGuiDraw() {
    }

    // Up to 1.21.5 text without alpha was drawn opaque; since 1.21.6 it is not drawn at all.
    public static int textColor(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    // drawString returned the x after the text, one more with a shadow; text() since 1.21.6 returns nothing.
    private static int end(int x, int width, boolean shadow) {
        return x + width + (shadow ? 1 : 0);
    }

    public static int text(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        return text(graphics, font, text, x, y, color, true);
    }

    public static int text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        return text(graphics, font, text, x, y, color, true);
    }

    public static int text(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        return text(graphics, font, text, x, y, color, true);
    }

    public static int text(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*graphics.text(font, text, x, y, textColor(color), shadow);
        return text == null ? x : end(x, font.width(text), shadow);
        *///?} else {
        return graphics.drawString(font, text, x, y, color, shadow);
        //?}
    }

    public static int text(GuiGraphics graphics, Font font, Component text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*graphics.text(font, text, x, y, textColor(color), shadow);
        return end(x, font.width(text), shadow);
        *///?} else {
        return graphics.drawString(font, text, x, y, color, shadow);
        //?}
    }

    public static int text(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        //? if >=26.1 {
        /*graphics.text(font, text, x, y, textColor(color), shadow);
        return end(x, font.width(text), shadow);
        *///?} else {
        return graphics.drawString(font, text, x, y, color, shadow);
        //?}
    }

    public static void centeredText(GuiGraphics graphics, Font font, String text, int centerX, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(font, text, centerX, y, textColor(color));
        *///?} else {
        graphics.drawCenteredString(font, text, centerX, y, color);
        //?}
    }

    public static void centeredText(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(font, text, centerX, y, textColor(color));
        *///?} else {
        graphics.drawCenteredString(font, text, centerX, y, color);
        //?}
    }

    public static void centeredText(GuiGraphics graphics, Font font, FormattedCharSequence text, int centerX, int y, int color) {
        //? if >=26.1 {
        /*graphics.centeredText(font, text, centerX, y, textColor(color));
        *///?} else {
        graphics.drawCenteredString(font, text, centerX, y, color);
        //?}
    }

    public static void render(Renderable renderable, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=26.1 {
        /*renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        renderable.render(graphics, mouseX, mouseY, partialTick);
        //?}
    }

    /** Draws part of a texture at its size: {@code width x height} pixels from (u, v). */
    public static void texture(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v,
                               int width, int height, int textureWidth, int textureHeight) {
        //? if >=26.1 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                textureWidth, textureHeight);
        *///?} else {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?}
    }

    /** Draws a {@code regionWidth x regionHeight} texture region scaled to {@code width x height}. */
    public static void texture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                               float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        //? if >=26.1 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                regionWidth, regionHeight, textureWidth, textureHeight);
        *///?} else {
        graphics.blit(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        //?}
    }

    /** Draws part of a texture tinted with an ARGB color. */
    public static void tintedTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v,
                                     int width, int height, int textureWidth, int textureHeight, int argb) {
        //? if >=26.1 {
        /*graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height,
                textureWidth, textureHeight, argb);
        *///?} else {
        graphics.setColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F,
                ((argb >>> 24) & 0xFF) / 255F);
        try {
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } finally {
            graphics.setColor(1F, 1F, 1F, 1F);
        }
        //?}
    }

    /** Draws an item with an alpha below 1; 26.1 has no tint for items and draws it opaque. */
    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y, float alpha) {
        //? if >=26.1 {
        /*graphics.item(stack, x, y);
        *///?} else {
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        try {
            graphics.renderItem(stack, x, y);
        } finally {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        //?}
    }

    public static void effectIcon(GuiGraphics graphics, net.minecraft.world.effect.MobEffect effect, int x, int y, int size) {
        //? if >=26.1 {
        /*graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                net.minecraft.client.gui.Gui.getMobEffectSprite(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)),
                x, y, size, size);
        *///?} else if >=1.20.5 {
        /*graphics.blit(x, y, 0, size, size, net.minecraft.client.Minecraft.getInstance().getMobEffectTextures()
                .get(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect)));
        *///?} else {
        graphics.blit(x, y, 0, size, size, net.minecraft.client.Minecraft.getInstance().getMobEffectTextures().get(effect));
        //?}
    }

    public static void rotate(GuiGraphics graphics, float degrees) {
        //? if >=26.1 {
        /*graphics.pose().rotate((float) Math.toRadians(degrees));
        *///?} else {
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(degrees));
        //?}
    }

    /**
     * Draws what follows above what was drawn so far. Up to 1.21.5 that is a step towards the viewer; since 1.21.6
     * the GUI has no depth and a new stratum is drawn above all earlier ones.
     */
    public static void raise(GuiGraphics graphics, float depth) {
        //? if >=26.1 {
        /*graphics.nextStratum();
        *///?} else {
        graphics.pose().translate(0F, 0F, depth);
        //?}
    }

    public static void wordWrap(GuiGraphics graphics, Font font, FormattedText text, int x, int y, int width, int color) {
        //? if >=26.1 {
        /*graphics.textWithWordWrap(font, text, x, y, width, textColor(color));
        *///?} else {
        graphics.drawWordWrap(font, text, x, y, width, color);
        //?}
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        //? if >=26.1 {
        /*graphics.outline(x, y, width, height, color);
        *///?} else {
        graphics.renderOutline(x, y, width, height, color);
        //?}
    }

    public static void item(GuiGraphics graphics, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        /*graphics.item(stack, x, y);
        *///?} else {
        graphics.renderItem(stack, x, y);
        //?}
    }

    public static void fakeItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        /*graphics.fakeItem(stack, x, y);
        *///?} else {
        graphics.renderFakeItem(stack, x, y);
        //?}
    }

    public static void itemDecorations(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        //? if >=26.1 {
        /*graphics.itemDecorations(font, stack, x, y);
        *///?} else {
        graphics.renderItemDecorations(font, stack, x, y);
        //?}
    }

    public static void itemDecorations(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, String countText) {
        //? if >=26.1 {
        /*graphics.itemDecorations(font, stack, x, y, countText);
        *///?} else {
        graphics.renderItemDecorations(font, stack, x, y, countText);
        //?}
    }

    // A tooltip wider than the room beside the cursor is wrapped to the wider side first. Vanilla and loaders only
    // flip such a tooltip, and renderers that restyle tooltips (ModernUI and the like) add their own padding, so an
    // unwrapped long line, such as an NBT dump, would leave the screen. The edge room covers border, padding and margin.
    private static final int TOOLTIP_CURSOR_GAP = 12;
    private static final int TOOLTIP_EDGE_ROOM = 16;
    private static final int TOOLTIP_MIN_WIDTH = 48;

    /** Widest tooltip text that fits beside the cursor on the side with more room. */
    public static int tooltipWrapWidth(GuiGraphics graphics, int mouseX) {
        int right = graphics.guiWidth() - mouseX - TOOLTIP_CURSOR_GAP - TOOLTIP_EDGE_ROOM;
        int left = mouseX - TOOLTIP_CURSOR_GAP - TOOLTIP_EDGE_ROOM;
        return Math.max(TOOLTIP_MIN_WIDTH, Math.max(right, left));
    }

    /** Splits lines wider than maxWidth, keeping their styles. */
    public static List<Component> fitTooltipLines(Font font, List<Component> lines, int maxWidth) {
        List<Component> fitted = new ArrayList<>(lines.size());
        for (Component line : lines) {
            if (font.width(line) <= maxWidth) {
                fitted.add(line);
                continue;
            }
            for (FormattedText part : font.getSplitter().splitLines(line, maxWidth, Style.EMPTY)) {
                MutableComponent piece = Component.empty();
                part.visit((style, text) -> {
                    piece.append(Component.literal(text).setStyle(style));
                    return Optional.empty();
                }, Style.EMPTY);
                fitted.add(piece);
            }
        }
        return fitted;
    }

    public static void tooltip(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        List<Component> lines = fitTooltipLines(font,
                Screen.getTooltipFromItem(Minecraft.getInstance(), stack), tooltipWrapWidth(graphics, mouseX));
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY,
                stack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE));
        *///?} else {
        graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
        //?}
    }

    public static void tooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        lines = fitTooltipLines(font, lines, tooltipWrapWidth(graphics, mouseX));
        //? if >=26.1 {
        /*graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        *///?} else {
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        //?}
    }

    // A separate name, since List<Component> and List<FormattedCharSequence> overloads would clash after erasure.
    public static void sequenceTooltip(GuiGraphics graphics, Font font, List<? extends FormattedCharSequence> lines, int mouseX, int mouseY) {
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        *///?} else {
        graphics.renderTooltip(font, lines, mouseX, mouseY);
        //?}
    }

    public static void push(GuiGraphics graphics) {
        //? if >=26.1 {
        /*graphics.pose().pushMatrix();
        *///?} else {
        graphics.pose().pushPose();
        //?}
    }

    public static void pop(GuiGraphics graphics) {
        //? if >=26.1 {
        /*graphics.pose().popMatrix();
        *///?} else {
        graphics.pose().popPose();
        //?}
    }

    // The GUI is two-dimensional since 1.21.6; z only ordered layers before and is dropped there.
    public static void translate(GuiGraphics graphics, double x, double y, double z) {
        //? if >=26.1 {
        /*graphics.pose().translate((float) x, (float) y);
        *///?} else {
        graphics.pose().translate(x, y, z);
        //?}
    }

    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        //? if >=26.1 {
        /*graphics.pose().scale(x, y);
        *///?} else {
        graphics.pose().scale(x, y, z);
        //?}
    }
}
