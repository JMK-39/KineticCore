package dev.xyat.kineticcore.api.client.theme;

import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * Central rendering theme and shared drawing helpers for Kinetic interfaces.
 */
public final class GuiTheme {
    /** Standard themed background surfaces available to state-aware business rendering. */
    public enum Surface {
        PANEL,
        PANEL_ALT,
        FIELD
    }

    /** Generic business-state indicators whose visual colors remain owned by the Kinetic theme. */
    public enum Indicator {
        INFO,
        SUCCESS,
        WARNING,
        DANGER,
        MUTED
    }

    /** Immutable color palette exposed for read-only theme-consistent custom rendering. */
    public record Palette(
            int background,
            int panel,
            int panelAlt,
            int border,
            int accent,
            int accentHover,
            int field,
            int text,
            int mutedText,
            int translatedText,
            int danger,
            int scrollTrack,
            int scrollThumb,
            int scrollThumbHover,
            int shadow
    ) {
    }

    /** Default Kinetic dark-blue-compatible palette used when no alternate theme is installed. */
    public static final Palette DEFAULT = new Palette(
            0xCC0A0A0A,
            0xE01B1B1B,
            0xE0262626,
            0xFF666666,
            0xFF777777,
            0xFFAAAAAA,
            0xE0101010,
            0xFFFFFFFF,
            0xFFB0B0B0,
            0xFF55FF55,
            0xFFE05A5A,
            0xFF171717,
            0xFFFF9800,
            0xFFFFD700,
            0xC0000000
    );

    private static volatile Palette current = DEFAULT;

    private static final int BORDER_NORMAL = 0xFFFFFFFF;
    private static final int BORDER_HOVER = 0xFF4DA6FF;
    private static final int BORDER_ERROR = 0xFFFF5555;
    private static final int BORDER_SELECTED = 0xFFFFAA00;

    private static final ResourceLocation ITEM_GRID_TEXTURE = KineticResourceIds.of("kineticcore", "textures/gui/item_selector_checkerboard.png");
    private static final int ITEM_GRID_TEXTURE_WIDTH = 475;
    private static final int ITEM_GRID_TEXTURE_HEIGHT = 304;

    private GuiTheme() {
    }

    /** Returns the active immutable Kinetic palette for theme-consistent custom rendering. */
    public static Palette current() {
        return current;
    }


    /** Resolves the standard outline color for the supplied control state. */
    private static int stateBorder(boolean selected, boolean hovered, boolean error) {
        if (selected) return BORDER_SELECTED;
        if (hovered) return BORDER_HOVER;
        if (error) return BORDER_ERROR;
        return BORDER_NORMAL;
    }

    /** Draws the standard full-canvas background using the active theme. */
    public static void canvasBackground(GuiGraphics graphics, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(0, 0, width, height, current.background());
    }

    /** Draws one standard themed surface without adding an outline. */
    public static void surface(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Surface surface
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, surfaceColor(surface));
    }

    /** Draws one themed surface with an explicit alpha multiplier. */
    public static void surface(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Surface surface,
            float alpha
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        int alphaByte = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int color = (surfaceColor(surface) & 0x00FFFFFF) | (alphaByte << 24);
        graphics.fill(x, y, x + width, y + height, color);
    }

    /** Draws a standard themed surface and applies selected, hovered, or error outline priority. */
    public static void stateSurface(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Surface surface,
            boolean selected,
            boolean hovered,
            boolean error
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, surfaceColor(surface));
        graphics.renderOutline(x, y, width, height, stateBorder(selected, hovered, error));
    }

    /** Draws a standard theme checkerboard using the active panel and alternate-panel colors. */
    public static void checkerboard(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int cellSize
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        int size = Math.max(1, cellSize);
        for (int yy = 0; yy < height; yy += size) {
            int drawHeight = Math.min(size, height - yy);
            for (int xx = 0; xx < width; xx += size) {
                int drawWidth = Math.min(size, width - xx);
                boolean primary = ((xx / size) + (yy / size)) % 2 == 0;
                graphics.fill(
                        x + xx,
                        y + yy,
                        x + xx + drawWidth,
                        y + yy + drawHeight,
                        primary ? current.panel() : current.panelAlt()
                );
            }
        }
    }

    /** Draws a generic RGB color swatch without exposing widget implementation details. */
    public static void colorSwatch(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int rgb,
            boolean outlined
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, 0xFF000000 | (rgb & 0xFFFFFF));
        if (outlined) stateOutline(graphics, x, y, width, height, false, false, false);
    }

    /** Draws a one-pixel horizontal separator using the active theme border color. */
    public static void separator(GuiGraphics graphics, int x, int y, int width) {
        if (graphics == null || width <= 0) return;
        graphics.fill(x, y, x + width, y + 1, current.border());
    }

    /** Draws a one-pixel vertical separator using the active theme border color. */
    public static void verticalSeparator(GuiGraphics graphics, int x, int y, int height) {
        if (graphics == null || height <= 0) return;
        graphics.fill(x, y, x + 1, y + height, current.border());
    }

    /** Draws the standard outer frame used by item and rule grids without adding per-cell backgrounds. */
    public static void gridFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, current.background());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, current.panelAlt());
        }
    }

    private static int surfaceColor(Surface surface) {
        return switch (Objects.requireNonNull(surface, "surface")) {
            case PANEL -> current.panel();
            case PANEL_ALT -> current.panelAlt();
            case FIELD -> current.field();
        };
    }

    /** Draws the standard selected, hovered, or error outline for a control. */
    public static void stateOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean selected,
            boolean hovered,
            boolean error
    ) {
        stateOutline(graphics, x, y, width, height, selected, hovered, error, 1);
    }

    /** Draws a standard state outline with a theme-owned color and caller-selected thickness. */
    public static void stateOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            boolean selected,
            boolean hovered,
            boolean error,
            int thickness
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        int layers = Math.max(1, thickness);
        int color = stateBorder(selected, hovered, error);
        for (int i = 0; i < layers && width - i * 2 > 0 && height - i * 2 > 0; i++) {
            graphics.renderOutline(x + i, y + i, width - i * 2, height - i * 2, color);
        }
    }

    /** Draws one standard semantic indicator outline without exposing raw border colors to business code. */
    public static void indicatorOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Indicator indicator
    ) {
        indicatorOutline(graphics, x, y, width, height, indicator, 1);
    }

    /** Draws a semantic indicator outline with a caller-selected thickness. */
    public static void indicatorOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Indicator indicator,
            int thickness
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        int layers = Math.max(1, thickness);
        int color = indicatorColor(indicator);
        for (int i = 0; i < layers && width - i * 2 > 0 && height - i * 2 > 0; i++) {
            graphics.renderOutline(x + i, y + i, width - i * 2, height - i * 2, color);
        }
    }

    /** Fills one generic semantic indicator rectangle using the active Kinetic theme. */
    public static void indicatorFill(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Indicator indicator
    ) {
        indicatorFill(graphics, x, y, width, height, indicator, 1.0F);
    }

    /** Fills one generic semantic indicator rectangle using an explicit alpha multiplier. */
    public static void indicatorFill(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            Indicator indicator,
            float alpha
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        int alphaByte = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        int color = (indicatorColor(indicator) & 0x00FFFFFF) | (alphaByte << 24);
        graphics.fill(x, y, x + width, y + height, color);
    }

    /** Returns the active theme color for a semantic indicator. */
    public static int indicatorColor(Indicator indicator) {
        return switch (Objects.requireNonNull(indicator, "indicator")) {
            case INFO -> BORDER_HOVER;
            case SUCCESS -> 0xFF55DD88;
            case WARNING -> BORDER_SELECTED;
            case DANGER -> BORDER_ERROR;
            case MUTED -> current.border();
        };
    }

    /** Draws a panel using the alternate panel background and standard border. */
    public static void panelAlt(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, current.panelAlt());
        graphics.renderOutline(x, y, width, height, current.border());
    }

    /** Draws the standard Kinetic panel using the active theme's panel background and border. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, current.panel());
        graphics.renderOutline(x, y, width, height, current.border());
    }

    /** Draws the standard full-surface Kinetic shadow. */
    public static void shadow(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, current.shadow());
    }

    /** Draws theme text with the requested opacity while the API owns blend-state handling. */
    public static void alphaText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            float alpha
    ) {
        if (graphics == null || font == null || text == null) return;
        float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
        if (clampedAlpha <= 0.0F) return;
        int alphaByte = Math.round(clampedAlpha * 255.0F);
        int color = (alphaByte << 24) | (current.text() & 0x00FFFFFF);
        KineticRenderRuntime.enableBlend();
        graphics.drawString(font, text, x, y, color);
        KineticRenderRuntime.disableBlend();
    }


    /** Runs one GUI rendering action with depth testing disabled, then restores the standard enabled state. */
    public static void runWithoutDepthTest(Runnable action) {
        Objects.requireNonNull(action, "action");
        KineticRenderRuntime.runWithoutDepthTest(action);
    }

    /** Restores the standard opaque-white GUI shader color before textured rendering. */
    public static void resetShaderColor() {
        KineticRenderRuntime.resetShaderColor();
    }

    /** Trims plain text to the requested pixel width using the supplied font. */
    public static String trim(Font font, String text, int width) {
        if (font == null || text == null || text.isEmpty()) return "";
        if (font.width(text) <= width) return text;
        int ellipsis = font.width("...");
        return font.plainSubstrByWidth(text, Math.max(0, width - ellipsis)) + "...";
    }

    /** Returns whether the supplied GUI-space point lies inside the rectangular bounds. */
    public static boolean hovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Draws a themed horizontal scrollbar for the supplied viewport and scroll state. */
    public static void horizontalScrollbar(
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbWidth,
            int maxOffset,
            double offset,
            boolean dragging
    ) {
        if (graphics == null || maxOffset <= 0 || width <= 0 || height <= 0) return;
        int safeThumb = Mth.clamp(thumbWidth, 1, width);
        double safeOffset = Math.max(0D, Math.min(offset, maxOffset));
        int thumbX = x + (int) Math.round((width - safeThumb) * (safeOffset / maxOffset));
        boolean hovered = hovering(mouseX, mouseY, thumbX, y, safeThumb, height);
        graphics.fill(x, y, x + width, y + height, current.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, current.scrollTrack());
        }
        graphics.fill(
                thumbX,
                y,
                thumbX + safeThumb,
                y + height,
                dragging || hovered ? current.scrollThumbHover() : current.scrollThumb()
        );
    }

    /** Draws a themed scrollbar for the supplied viewport and scroll state. */
    public static void scrollbar(
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
        if (controller == null || graphics == null) return;
        controller.render(
                graphics,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                minThumbHeight
        );
    }

    /** Draws a themed scrollbar for the supplied viewport and scroll state. */
    public static void scrollbar(
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbHeight,
            int maxOffset,
            int offset,
            boolean dragging
    ) {
        scrollbar(graphics, mouseX, mouseY, x, y, width, height, thumbHeight, maxOffset, (double) offset, dragging);
    }

    /** Draws a themed scrollbar for the supplied viewport and scroll state. */
    public static void scrollbar(
            GuiGraphics graphics,
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height,
            int thumbHeight,
            int maxOffset,
            double offset,
            boolean dragging
    ) {
        if (graphics == null || maxOffset <= 0 || width <= 0 || height <= 0) return;
        int safeThumb = Mth.clamp(thumbHeight, 1, height);
        double safeOffset = Math.max(0D, Math.min(offset, maxOffset));
        int thumbY = y + (int) Math.round((height - safeThumb) * (safeOffset / maxOffset));
        boolean hovered = hovering(mouseX, mouseY, x, thumbY, width, safeThumb);
        graphics.fill(x, y, x + width, y + height, current.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, current.scrollTrack());
        }
        graphics.fill(
                x,
                thumbY,
                x + width,
                thumbY + safeThumb,
                dragging || hovered ? current.scrollThumbHover() : current.scrollThumb()
        );
    }

    /** Draws one standard 18x18 item slot in its normal state. */
    public static void itemSlot(GuiGraphics graphics, int x, int y) {
        itemSlot(graphics, x, y, false);
    }

    /** Draws a standard-size themed item slot. */
    public static void itemSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        itemSlot(graphics, x, y, 18, 18, 4, false, hovered, false);
    }


    /** Draws a square item slot using the standard checker cell size. */
    public static void itemSlot(GuiGraphics graphics, int x, int y, int size, boolean hovered) {
        itemSlot(graphics, x, y, size, 4, hovered);
    }

    /** Draws a themed item slot with explicit slot and cell sizes. */
    public static void itemSlot(GuiGraphics graphics, int x, int y, int size, int cellSize, boolean hovered) {
        itemSlot(graphics, x, y, size, size, cellSize, false, hovered, false);
    }

    /** Draws one item slot with explicit dimensions and state flags. */
    public static void itemSlot(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int cellSize,
            boolean selected,
            boolean hovered,
            boolean error
    ) {
        if (graphics == null || width <= 0 || height <= 0) return;
        drawItemGridTexture(graphics, x, y, width, height, 0, 0, 18, 18);
        stateOutline(graphics, x, y, width, height, selected, hovered, error);
    }

    /** Draws the standard item-grid background for the exact bounds. */
    public static void itemGrid(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        drawItemGridTexture(graphics, x, y, width, height, 1, 1, 16, 16);
    }

    /** Draws an item-selector grid with explicit column and row counts. */
    public static void itemSelectorGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
        if (graphics == null || columns <= 0 || rows <= 0) return;
        int width = Math.min(ITEM_GRID_TEXTURE_WIDTH, columns * 19);
        int height = Math.min(ITEM_GRID_TEXTURE_HEIGHT, rows * 19);
        graphics.blit(
                ITEM_GRID_TEXTURE,
                x,
                y,
                0,
                0,
                width,
                height,
                ITEM_GRID_TEXTURE_WIDTH,
                ITEM_GRID_TEXTURE_HEIGHT
        );
    }

    /** Renders an item stack using the standard Kinetic slot sizing and decoration rules. */
    public static void item(
            GuiGraphics graphics,
            Font font,
            ItemStack stack,
            int x,
            int y,
            int slotSize,
            float scale,
            boolean decorations
    ) {
        if (graphics == null || stack == null || stack.isEmpty() || scale <= 0f) return;
        float renderSize = 16f * scale;
        float offset = (slotSize - renderSize) / 2f;
        KineticRenderRuntime.enableDepthTest();
        graphics.pose().pushPose();
        graphics.pose().translate(x + offset, y + offset, 0);
        graphics.pose().scale(scale, scale, 1f);
        graphics.renderItem(stack, 0, 0);
        if (decorations && font != null) graphics.renderItemDecorations(font, stack, 0, 0);
        graphics.pose().popPose();
        KineticRenderRuntime.disableDepthTest();
    }

    private static void drawItemGridTexture(
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
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(width / (float) sourceWidth, height / (float) sourceHeight, 1f);
        graphics.blit(
                ITEM_GRID_TEXTURE,
                0,
                0,
                sourceX,
                sourceY,
                sourceWidth,
                sourceHeight,
                ITEM_GRID_TEXTURE_WIDTH,
                ITEM_GRID_TEXTURE_HEIGHT
        );
        graphics.pose().popPose();
    }
}
