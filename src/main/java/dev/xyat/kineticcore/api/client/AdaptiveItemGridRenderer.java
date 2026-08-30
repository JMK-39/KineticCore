package dev.xyat.kineticcore.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class AdaptiveItemGridRenderer {
    private static final ResourceLocation CHECKERBOARD_TEXTURE = new ResourceLocation("kineticcore", "textures/gui/item_selector_checkerboard.png");
    private static final int TEXTURE_WIDTH = 475;
    private static final int TEXTURE_HEIGHT = 304;
    private static final int BASE_SLOT_SIZE = 18;
    private static final int HOVER_BORDER = 0xFF66CCFF;

    private AdaptiveItemGridRenderer() {
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y) {
        drawSlot(graphics, x, y, BASE_SLOT_SIZE, 4, false);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        drawSlot(graphics, x, y, BASE_SLOT_SIZE, 4, hovered);
    }

    public static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            int cellSize,
            boolean hovered
    ) {
        drawSlot(graphics, x, y, size, size, cellSize, hovered);
    }

    public static void drawSlot(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int cellSize,
            boolean hovered
    ) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }

        drawTextureRegion(graphics, x, y, width, height, 0, 0, BASE_SLOT_SIZE, BASE_SLOT_SIZE);

        if (hovered) {
            graphics.renderOutline(x, y, width, height, HOVER_BORDER);
        }
    }

    public static void drawSlot(GuiGraphics graphics, ItemStack stack, int x, int y) {
        drawSlot(graphics, x, y, BASE_SLOT_SIZE, 4, false);
    }

    public static void drawSlot(GuiGraphics graphics, ItemStack stack, int x, int y, boolean hovered) {
        drawSlot(graphics, x, y, BASE_SLOT_SIZE, 4, hovered);
    }

    public static void drawSlot(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int size,
            int cellSize,
            boolean hovered
    ) {
        drawSlot(graphics, x, y, size, cellSize, hovered);
    }

    public static void drawSlot(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int width,
            int height,
            int cellSize,
            boolean hovered
    ) {
        drawSlot(graphics, x, y, width, height, cellSize, hovered);
    }

    public static void drawGrid(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int cellSize
    ) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }

        drawTextureRegion(graphics, x, y, width, height, 1, 1, 16, 16);
    }

    public static void drawGrid(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int width,
            int height,
            int cellSize,
            boolean hovered
    ) {
        drawGrid(graphics, x, y, width, height, cellSize);
    }

    public static void drawItemSelectorGrid(GuiGraphics graphics, int x, int y) {
        drawItemSelectorGrid(graphics, x, y, 25, 16);
    }

    public static void drawItemSelectorGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
        if (graphics == null || columns <= 0 || rows <= 0) {
            return;
        }

        int width = Math.min(TEXTURE_WIDTH, columns * 19);
        int height = Math.min(TEXTURE_HEIGHT, rows * 19);

        graphics.blit(
                CHECKERBOARD_TEXTURE,
                x,
                y,
                0,
                0,
                width,
                height,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    private static void drawTextureRegion(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(
                width / (float) sourceWidth,
                height / (float) sourceHeight,
                1.0F
        );
        graphics.blit(
                CHECKERBOARD_TEXTURE,
                0,
                0,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
        graphics.pose().popPose();
    }

    public static void renderItem(
            GuiGraphics graphics,
            Font font,
            ItemStack stack,
            int x,
            int y,
            int slotSize,
            float scale,
            boolean decorations
    ) {
        if (graphics == null || stack == null || stack.isEmpty() || scale <= 0.0F) {
            return;
        }

        float renderSize = 16.0F * scale;
        float offset = (slotSize - renderSize) / 2.0F;

        RenderSystem.enableDepthTest();

        graphics.pose().pushPose();
        graphics.pose().translate(x + offset, y + offset, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);

        graphics.renderItem(stack, 0, 0);

        if (decorations && font != null) {
            graphics.renderItemDecorations(font, stack, 0, 0);
        }

        graphics.pose().popPose();
        RenderSystem.disableDepthTest();
    }
}
