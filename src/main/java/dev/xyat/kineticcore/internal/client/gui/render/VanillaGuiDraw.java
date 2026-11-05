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
import dev.xyat.kineticcore.internal.client.gui.text.LineBreaks;
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

    /** Wrapped text, one line under the other as vanilla draws it, with the punctuation rules of {@link LineBreaks}. */
    public static void wordWrap(GuiGraphics graphics, Font font, FormattedText text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : LineBreaks.split(font, text, width)) {
            text(graphics, font, line, x, y, color, false);
            y += font.lineHeight;
        }
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
    // Room the placement keeps between the cursor and a tooltip beside it (see placedMouse).
    private static final int TOOLTIP_CURSOR_GAP = 14;
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
            for (FormattedText part : LineBreaks.splitLines(font, line, maxWidth)) {
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

    /**
     * The thing the mouse is over, in screen coordinates. A tooltip is placed beside it instead of over it: below or
     * above a control, to the right or left of anything else, and always fully on screen. Without an anchor the
     * tooltip keeps clear of an item-slot-sized box around the cursor.
     */
    public record TooltipAnchor(int left, int top, int right, int bottom, boolean control) {
    }

    private static TooltipAnchor tooltipAnchor;

    /** Sets the anchor for the tooltips drawn until it is cleared with {@code null}. */
    public static void tooltipAnchor(TooltipAnchor anchor) {
        tooltipAnchor = anchor;
    }

    private static final int TOOLTIP_SCREEN_MARGIN = 2;
    // The tooltip background reaches 3 to 4 pixels past its content on every side.
    private static final int TOOLTIP_FRAME = 4;
    private static final int TOOLTIP_ANCHOR_GAP = 2;
    private static final int TOOLTIP_CURSOR_BOX = 18;

    /**
     * The mouse position to hand vanilla so that it draws a {@code width × height} tooltip where it covers neither
     * the anchor nor the screen edge (vanilla puts it 12 px right of and 12 px above the mouse).
     */
    private static int[] placedMouse(GuiGraphics graphics, int mouseX, int mouseY, int width, int height) {
        int screenWidth = graphics.guiWidth(), screenHeight = graphics.guiHeight();
        TooltipAnchor anchor = tooltipAnchor != null ? tooltipAnchor : new TooltipAnchor(mouseX - TOOLTIP_CURSOR_BOX,
                mouseY - TOOLTIP_CURSOR_BOX, mouseX + TOOLTIP_CURSOR_BOX, mouseY + TOOLTIP_CURSOR_BOX, false);
        int frameWidth = width + TOOLTIP_FRAME * 2, frameHeight = height + TOOLTIP_FRAME * 2;
        int minX = TOOLTIP_SCREEN_MARGIN, maxX = screenWidth - TOOLTIP_SCREEN_MARGIN - frameWidth;
        int minY = TOOLTIP_SCREEN_MARGIN, maxY = screenHeight - TOOLTIP_SCREEN_MARGIN - frameHeight;
        // Below or above a control it starts at the control's left edge; below or above a row it starts at the mouse.
        int alignedX = clampTooltip(anchor.control() ? anchor.left() : mouseX - TOOLTIP_FRAME * 2, minX, maxX);
        int alignedY = clampTooltip(anchor.top(), minY, maxY);
        int[] right = {anchor.right() + TOOLTIP_ANCHOR_GAP, alignedY};
        int[] left = {anchor.left() - TOOLTIP_ANCHOR_GAP - frameWidth, alignedY};
        int[] below = {alignedX, anchor.bottom() + TOOLTIP_ANCHOR_GAP};
        int[] above = {alignedX, anchor.top() - TOOLTIP_ANCHOR_GAP - frameHeight};
        int[][] order = anchor.control() ? new int[][]{below, above, right, left} : new int[][]{right, left, below, above};
        int[] frame = null;
        for (int[] candidate : order) {
            if (candidate[0] >= minX && candidate[0] <= maxX && candidate[1] >= minY && candidate[1] <= maxY) {
                frame = candidate;
                break;
            }
        }
        // Nothing fits beside the anchor: stay on screen, overlapping it as little as the room allows.
        if (frame == null) frame = new int[]{clampTooltip(right[0], minX, maxX), alignedY};
        return new int[]{frame[0] + TOOLTIP_FRAME - 12, frame[1] + TOOLTIP_FRAME + 12};
    }

    private static int clampTooltip(int value, int min, int max) {
        return max < min ? min : Math.max(min, Math.min(max, value));
    }

    // Vanilla tooltip size: the widest component, and 10 px per text line (a single line is 8 px tall).
    private static int tooltipHeight(int textLines, int imageHeight) {
        int components = textLines + (imageHeight > 0 ? 1 : 0);
        return textLines * 10 + imageHeight - (components == 1 && imageHeight == 0 ? 2 : 0);
    }

    public static void tooltip(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        itemTooltip(graphics, font, stack, Screen.getTooltipFromItem(Minecraft.getInstance(), stack), mouseX, mouseY);
    }

    /** Renders prepared native lines while retaining the stack's tooltip image and style. */
    public static void itemTooltip(GuiGraphics graphics, Font font, ItemStack stack, List<Component> nativeLines, int mouseX, int mouseY) {
        List<Component> lines = fitTooltipLines(font, nativeLines, tooltipWrapWidth(graphics, mouseX));
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));
        int imageHeight = 0;
        var image = stack.getTooltipImage();
        if (image.isPresent()) {
            try {
                var component = net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(image.get());
                width = Math.max(width, component.getWidth(font));
                //? if >=26.1 {
                /*imageHeight = component.getHeight(font);
                *///?} else {
                imageHeight = component.getHeight();
                //?}
            } catch (RuntimeException ignored) {
                // A tooltip image without a client factory is drawn by vanilla as usual; only its size is unknown.
            }
        }
        int[] placed = placedMouse(graphics, mouseX, mouseY, width, tooltipHeight(lines.size(), imageHeight));
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, image, stack, placed[0], placed[1],
                stack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE));
        *///?} else {
        graphics.renderTooltip(font, lines, image, stack, placed[0], placed[1]);
        //?}
    }

    public static void tooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        lines = fitTooltipLines(font, lines, tooltipWrapWidth(graphics, mouseX));
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));
        int[] placed = placedMouse(graphics, mouseX, mouseY, width, tooltipHeight(lines.size(), 0));
        //? if >=26.1 {
        /*graphics.setComponentTooltipForNextFrame(font, lines, placed[0], placed[1]);
        *///?} else {
        graphics.renderComponentTooltip(font, lines, placed[0], placed[1]);
        //?}
    }

    // A separate name, since List<Component> and List<FormattedCharSequence> overloads would clash after erasure.
    public static void sequenceTooltip(GuiGraphics graphics, Font font, List<? extends FormattedCharSequence> lines, int mouseX, int mouseY) {
        // Lines wider than the room beside the cursor are rebuilt with their styles and wrapped, so the tooltip
        // stays inside the screen; only its height may exceed it, as in vanilla.
        int fitWidth = tooltipWrapWidth(graphics, mouseX);
        List<FormattedCharSequence> fitted = new ArrayList<>(lines.size());
        for (FormattedCharSequence line : lines) {
            if (font.width(line) <= fitWidth) {
                fitted.add(line);
                continue;
            }
            net.minecraft.network.chat.MutableComponent rebuilt = Component.empty();
            line.accept((index, style, codePoint) -> {
                rebuilt.append(Component.literal(new String(Character.toChars(codePoint))).setStyle(style));
                return true;
            });
            fitted.addAll(LineBreaks.split(font, rebuilt, fitWidth));
        }
        lines = fitted;
        int width = 0;
        for (FormattedCharSequence line : lines) width = Math.max(width, font.width(line));
        int[] placed = placedMouse(graphics, mouseX, mouseY, width, tooltipHeight(lines.size(), 0));
        //? if >=26.1 {
        /*graphics.setTooltipForNextFrame(font, lines, placed[0], placed[1]);
        *///?} else {
        graphics.renderTooltip(font, lines, placed[0], placed[1]);
        //?}
    }

    private static int batchDepth;

    /**
     * Runs {@code draw} with its fills collected and sent to the GPU together. Up to 1.21.5 every unbatched
     * {@code fill} is its own draw call, and a rounded shape is dozens of fills, so whole screens of slots and buttons
     * cost thousands of draw calls a frame. Nested calls join the outermost batch. Textures and items inside still draw
     * at once, so a batch should hold only fills that belong above the textures drawn with them. Since 1.21.6 the GUI
     * is collected and drawn in batches by the game anyway.
     */
    public static void batched(GuiGraphics graphics, Runnable draw) {
        //? if >=26.1 {
        /*draw.run();
        *///?} else {
        if (batchDepth > 0) {
            draw.run();
            return;
        }
        batchDepth++;
        try {
            graphics.drawManaged(draw);
        } finally {
            batchDepth--;
        }
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

    /**
     * Whether the mouse is over a rectangle given in the graphics' current local coordinates. Only true while a
     * screen is open, so HUD text never reacts to the cursor.
     */
    public static boolean mouseOver(GuiGraphics graphics, int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (graphics == null || minecraft.screen == null || width <= 0 || height <= 0) return false;
        var window = minecraft.getWindow();
        if (window.getScreenWidth() <= 0 || window.getScreenHeight() <= 0) return false;
        float mouseX = (float) (minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        float mouseY = (float) (minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        //? if >=26.1 {
        /*org.joml.Vector2f local = new org.joml.Matrix3x2f(graphics.pose()).invert()
                .transformPosition(new org.joml.Vector2f(mouseX, mouseY));
        float localX = local.x;
        float localY = local.y;
        *///?} else {
        org.joml.Vector4f local = new org.joml.Matrix4f(graphics.pose().last().pose()).invert()
                .transform(new org.joml.Vector4f(mouseX, mouseY, 0F, 1F));
        float localX = local.x();
        float localY = local.y();
        //?}
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }

    /** Horizontal scale of the current pose: how many GUI units one local unit covers. */
    public static float poseScale(GuiGraphics graphics) {
        //? if >=26.1 {
        /*return (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01());
        *///?} else {
        var pose = graphics.pose().last().pose();
        return (float) Math.hypot(pose.m00(), pose.m01());
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
