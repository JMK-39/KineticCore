package dev.xyat.kineticcore.internal.client.gui.theme;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme.Indicator;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme.Palette;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme.Surface;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;

import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.GridScrollController;
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




    private static final Palette CLASSIC = KineticTheme.DEFAULT;
    /**
     * The modern palette: opaque dark panels (the game shows only around the canvas, live and dimmed), white-tinted
     * edges.
     */
    private static final Palette MODERN = new Palette(
            0xFF0A0D12,
            0xFF15191F,
            0xFF22262E,
            0x50FFFFFF,
            CLASSIC.accent(),
            CLASSIC.accentHover(),
            0xFF101319,
            CLASSIC.text(),
            CLASSIC.mutedText(),
            CLASSIC.translatedText(),
            CLASSIC.danger(),
            0x50000000,
            CLASSIC.scrollThumb(),
            CLASSIC.scrollThumbHover(),
            0x90000000
    );
    /** A normal (idle) modern edge: a faint light line instead of the classic white frame. */
    private static final int MODERN_EDGE = 0x48FFFFFF;
    private static final int MODERN_SEPARATOR = 0x40FFFFFF;
    private static final float PANEL_RADIUS = 6F;
    private static final float SURFACE_RADIUS = 4F;

    private static final int BORDER_NORMAL = 0xFFFFFFFF;
    private static final int BORDER_HOVER = 0xFF4DA6FF;
    private static final int BORDER_ERROR = 0xFFFF5555;
    private static final int BORDER_SELECTED = 0xFFFFAA00;
    private static final int FIELD_TEXT = 0xFF55FFFF;
    private static final int FIELD_MUTED_TEXT = 0xFF55FFFF;
    /** 输入框占位提示：纯白，仅在空且无焦点时显示 / Input placeholder: pure white, only while empty and unfocused. */
    private static final int FIELD_PLACEHOLDER_TEXT = 0xFFFFFFFF;
    /** 输入框内容等于默认值：青色 / Input value equal to its default: cyan. */
    private static final int FIELD_DEFAULT_TEXT = 0xFF55FFFF;
    /** 输入框内容已修改（输入了内容）：绿色 / Input value modified (content entered): green. */
    private static final int FIELD_MODIFIED_TEXT = 0xFF55FF55;
    private static final int FIELD_ERROR_TEXT = 0xFFFF5555;

    private static final ResourceLocation ITEM_SLOT_TEXTURE = KineticResourceIds.of("kineticcore", "textures/gui/item_slot.png");
    private static final int ITEM_SLOT_TEXTURE_SIZE = 8;
    private static final int ITEM_CHECKER_CELL_SIZE = 2;

    private GuiTheme() {
    }

    /** Returns the active immutable Kinetic palette for theme-consistent custom rendering. */
    public static Palette current() {
        return modern() ? MODERN : CLASSIC;
    }

    /** Whether the modern (rounded) style is on. */
    public static boolean modern() {
        return GuiStyle.modern();
    }

    /** Corner radius for a modern shape: the standard radius, smaller on small shapes so they never become pills. */
    private static float radius(int width, int height, float preferred) {
        return Math.min(preferred, Math.min(width, height) / 3F);
    }

    /** Outline width in physical pixels for a state outline (hover, selection, error): about two thirds of a GUI pixel. */
    private static float stateThickness(GuiGraphics graphics, int layers) {
        return Math.max(1F, Math.round(RoundedShapes.pixelScale(graphics) * 0.67F)) * Math.max(1, layers);
    }

    /** A selected outline is a full GUI pixel wide, a hovered one two thirds, so the current choice stands out more. */
    private static float stateThickness(GuiGraphics graphics, int layers, boolean selected) {
        if (!selected) return stateThickness(graphics, layers);
        return Math.max(1F, Math.round(RoundedShapes.pixelScale(graphics))) * Math.max(1, layers);
    }

    /**
     * Fills a background shape: a rounded rectangle in the modern style, a plain rectangle in the classic one. For rows
     * and cells whose outline comes from {@link #stateOutline}, so fill and outline share the same corners.
     */
    public static void fillShape(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (graphics == null || width <= 0 || height <= 0) return;
        if (modern()) RoundedShapes.fill(graphics, x, y, width, height, radius(width, height, SURFACE_RADIUS), color);
        else graphics.fill(x, y, x + width, y + height, color);
    }

    /** Fills from (x1, y1) to (x2, y2) as a pill (fully rounded ends) in the modern style; a plain rectangle in the classic one. */
    public static void pillRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null || x2 <= x1 || y2 <= y1) return;
        if (modern()) RoundedShapes.fill(graphics, x1, y1, x2 - x1, y2 - y1, Math.min(x2 - x1, y2 - y1) / 2F, color);
        else graphics.fill(x1, y1, x2, y2, color);
    }

    /**
     * The modern hover and selection highlight: a soft halo just outside the shape that breathes with the outline,
     * and, for surfaces drawn under their content, a faint tint of the state colour inside.
     */
    private static void stateGlow(GuiGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered,
                                  boolean tintInside) {
        if (!selected && !hovered) return;
        int base = (selected ? BORDER_SELECTED : BORDER_HOVER) & 0x00FFFFFF;
        float breath = SelectionPulse.level(System.nanoTime() / 1_000_000L);
        float r = radius(width, height, SURFACE_RADIUS);
        float px = RoundedShapes.pixelScale(graphics);
        if (tintInside) {
            int tint = Math.round((selected ? 0x26 : 0x1C) + breath * 0x1C);
            RoundedShapes.fill(graphics, x, y, width, height, r, tint << 24 | base);
        }
        int near = Math.round((selected ? 0x70 : 0x58) + breath * 0x50);
        int far = near / 3;
        RoundedShapes.outline(graphics, x - 1, y - 1, width + 2, height + 2, r + 1, px, near << 24 | base);
        RoundedShapes.outline(graphics, x - 2, y - 2, width + 4, height + 4, r + 2, px, far << 24 | base);
    }

    private static void outlineShape(GuiGraphics graphics, int x, int y, int width, int height, int color, float thickness) {
        if (modern()) RoundedShapes.outline(graphics, x, y, width, height, radius(width, height, SURFACE_RADIUS), thickness, color);
        else VanillaGuiDraw.outline(graphics, x, y, width, height, color);
    }

    /** Returns the standard cyan text color used on the light input surface. */
    public static int fieldText() {
        return FIELD_TEXT;
    }

    /** Returns the standard cyan read-only text color used on the light input surface. */
    public static int fieldMutedText() {
        return FIELD_MUTED_TEXT;
    }

    /** 输入框占位提示颜色（纯白）/ Input placeholder color (pure white). */
    public static int fieldPlaceholderText() {
        return FIELD_PLACEHOLDER_TEXT;
    }

    /** 输入框内容等于默认值时的颜色（青色）/ Input text color when the value equals its default (cyan). */
    public static int fieldDefaultText() {
        return FIELD_DEFAULT_TEXT;
    }

    /** 输入框内容已修改时的颜色（绿）/ Input text color when the value was modified (green). */
    public static int fieldModifiedText() {
        return FIELD_MODIFIED_TEXT;
    }

    /** Input text color while validation reports an error (red). */
    public static int fieldErrorText() {
        return FIELD_ERROR_TEXT;
    }

    /** Resolves the standard outline color for the supplied control state. */
    private static int stateBorder(boolean selected, boolean hovered, boolean error) {
        return stateBorder(selected, hovered, error, System.nanoTime() / 1_000_000L);
    }

    private static int stateBorder(boolean selected, boolean hovered, boolean error, long millis) {
        if (selected) return SelectionPulse.color(BORDER_SELECTED, millis);
        if (hovered) return SelectionPulse.color(BORDER_HOVER, millis);
        if (error) return BORDER_ERROR;
        return BORDER_NORMAL;
    }

    /** Draws the standard full-canvas background using the active theme. */
    public static void canvasBackground(GuiGraphics graphics, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.backdrop(graphics, 0, 0, width, height);
        graphics.fill(0, 0, width, height, current().background());
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
        surfaceFill(graphics, x, y, width, height, surfaceColor(surface));
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
        surfaceFill(graphics, x, y, width, height, color);
    }

    /** A themed surface dark enough to read text on counts as a background for the layout check; translucent ones do not. */
    private static void surfaceFill(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if ((color >>> 24) >= 0xB0) dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.backdrop(graphics, x, y, width, height);
        fillShape(graphics, x, y, width, height, color);
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
        fillShape(graphics, x, y, width, height, surfaceColor(surface));
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.frame(graphics, x, y, width, height);
        if (modern() && !selected && !hovered && !error) {
            outlineShape(graphics, x, y, width, height, MODERN_EDGE, 1F);
            return;
        }
        if (modern()) stateGlow(graphics, x, y, width, height, selected, hovered, true);
        outlineShape(graphics, x, y, width, height, stateBorder(selected, hovered, error), stateThickness(graphics, 1, selected));
    }

    /** 选中边框的反色，用于中键跳转后的边框闪烁 / Inverse of the selected border color, used by the jump flash. */
    private static final int BORDER_SELECTED_INVERSE = 0xFF000000 | (~BORDER_SELECTED & 0x00FFFFFF);

    /**
     * 中键跳转后的边框闪烁：选中边框（橘黄色）在闪烁阶段切换为其反色，格子本身不变。
     * 所有列表/网格/Tab/行内按钮统一走这里，只在闪烁的“亮”阶段调用。
     * Jump flash: during the flash phase the selection border (orange) switches to its inverse color; the cell itself
     * is unchanged. Shared by every list, grid, tab and row button; called only during the "on" phase.
     */
    public static void selectionFlash(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, 0.0F, 0.0F, 200.0F);
        outlineShape(graphics, x, y, width, height, BORDER_SELECTED_INVERSE, stateThickness(graphics, 1));
        VanillaGuiDraw.pop(graphics);
    }

    /** 闪烁期间文字颜色：边框闪烁不改变文字 / Text color during the flash; the border flash keeps normal text. */
    public static int selectionFlashText() {
        return current().text();
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
                        primary ? current().panel() : current().panelAlt()
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

    /** Draws a one-pixel gray-white divider between content regions. */
    public static void separator(GuiGraphics graphics, int x, int y, int width) {
        if (graphics == null || width <= 0) return;
        graphics.fill(x, y, x + width, y + 1, modern() ? MODERN_SEPARATOR : CLASSIC.accentHover());
    }

    /** Draws a one-pixel gray-white divider between content regions. */
    public static void verticalSeparator(GuiGraphics graphics, int x, int y, int height) {
        if (graphics == null || height <= 0) return;
        graphics.fill(x, y, x + 1, y + height, modern() ? MODERN_SEPARATOR : CLASSIC.accentHover());
    }

    /** Draws an open grid region; cards provide their own interactive outlines. */
    public static void gridFrame(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        surfaceFill(graphics, x, y, width, height, current().panelAlt());
    }

    private static int surfaceColor(Surface surface) {
        return switch (Objects.requireNonNull(surface, "surface")) {
            case PANEL -> current().panel();
            case PANEL_ALT -> current().panelAlt();
            case FIELD -> current().field();
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
        if (modern()) {
            if (!selected && !hovered && !error) {
                outlineShape(graphics, x, y, width, height, MODERN_EDGE, 1F);
                return;
            }
            // Drawn over the content (buttons, rows), so only the halo outside, no tint over the label.
            stateGlow(graphics, x, y, width, height, selected, hovered, false);
            outlineShape(graphics, x, y, width, height, stateBorder(selected, hovered, error), stateThickness(graphics, layers, selected));
            return;
        }
        int color = stateBorder(selected, hovered, error);
        for (int i = 0; i < layers && width - i * 2 > 0 && height - i * 2 > 0; i++) {
            VanillaGuiDraw.outline(graphics, x + i, y + i, width - i * 2, height - i * 2, color);
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
        if (indicator == Indicator.SUCCESS || indicator == Indicator.WARNING)
            color = SelectionPulse.color(color, System.nanoTime() / 1_000_000L);
        if (modern()) {
            outlineShape(graphics, x, y, width, height, color, stateThickness(graphics, layers));
            return;
        }
        for (int i = 0; i < layers && width - i * 2 > 0 && height - i * 2 > 0; i++) {
            VanillaGuiDraw.outline(graphics, x + i, y + i, width - i * 2, height - i * 2, color);
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
        fillShape(graphics, x, y, width, height, color);
    }

    /** Returns the active theme color for a semantic indicator. */
    public static int indicatorColor(Indicator indicator) {
        return switch (Objects.requireNonNull(indicator, "indicator")) {
            case INFO -> BORDER_HOVER;
            case SUCCESS -> 0xFF55DD88;
            case WARNING -> BORDER_SELECTED;
            case DANGER -> BORDER_ERROR;
            case MUTED -> current().border();
        };
    }

    /** Draws a panel using the alternate panel background and standard border. */
    public static void panelAlt(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.frame(graphics, x, y, width, height);
        if (modern()) {
            fillShape(graphics, x, y, width, height, MODERN.panelAlt());
            outlineShape(graphics, x, y, width, height, MODERN_EDGE, 1F);
            return;
        }
        graphics.fill(x, y, x + width, y + height, CLASSIC.panelAlt());
        VanillaGuiDraw.outline(graphics, x, y, width, height, CLASSIC.border());
    }

    /** Draws the outer canvas or modal panel with a single white outline. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.frame(graphics, x, y, width, height);
        if (modern()) {
            // A soft shadow under the panel, then a faint light rim.
            float r = radius(width, height, PANEL_RADIUS);
            for (int i = 3; i >= 1; i--) {
                RoundedShapes.fill(graphics, x - i, y - i + 1, width + i * 2, height + i * 2, r + i, (0x14 * (4 - i)) << 24);
            }
            RoundedShapes.fill(graphics, x, y, width, height, r, MODERN.panel());
            RoundedShapes.outline(graphics, x, y, width, height, r, 1F, 0x60FFFFFF);
            return;
        }
        graphics.fill(x, y, x + width, y + height, CLASSIC.panel());
        VanillaGuiDraw.outline(graphics, x, y, width, height, BORDER_NORMAL);
    }

    /** Draws the standard full-surface Kinetic shadow. */
    public static void shadow(GuiGraphics graphics, int width, int height) {
        // Inside a canvas page the runtime already dims the whole screen for it; see KineticScreenRuntime.
        if (dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenRuntime.pageShadowCoveredByScreen()) return;
        shadowFill(graphics, width, height);
    }

    /** Fills the theme shadow unconditionally, in the graphics' own coordinates. */
    public static void shadowFill(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, current().shadow());
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
        int color = (alphaByte << 24) | (current().text() & 0x00FFFFFF);
        KineticRenderRuntime.enableBlend();
        VanillaGuiDraw.text(graphics, font, text, x, y, color);
        KineticRenderRuntime.disableBlend();
    }


    /** Returns whether the supplied GUI-space point lies inside the rectangular bounds. */
    public static boolean hovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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
        if (modern()) {
            // A thin pill track with a pill thumb.
            RoundedShapes.fill(graphics, x, y, width, height, width / 2F, MODERN.scrollTrack());
            RoundedShapes.fill(graphics, x, thumbY, width, safeThumb, width / 2F,
                    dragging || hovered ? MODERN.scrollThumbHover() : MODERN.scrollThumb());
            return;
        }
        graphics.fill(x, y, x + width, y + height, CLASSIC.border());
        if (width > 2 && height > 2) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, CLASSIC.scrollTrack());
        }
        graphics.fill(
                x,
                thumbY,
                x + width,
                thumbY + safeThumb,
                dragging || hovered ? CLASSIC.scrollThumbHover() : CLASSIC.scrollThumb()
        );
    }

    /** Draws one standard 18x18 item slot in its normal state. */
    public static void itemSlot(GuiGraphics graphics, int x, int y) {
        itemSlot(graphics, x, y, false);
    }

    /** Draws a standard-size themed item slot. */
    public static void itemSlot(GuiGraphics graphics, int x, int y, boolean hovered) {
        itemSlot(graphics, x, y, 18, 18, ITEM_CHECKER_CELL_SIZE, false, hovered, false);
    }


    /** Draws a square item slot using the standard checker cell size. */
    public static void itemSlot(GuiGraphics graphics, int x, int y, int size, boolean hovered) {
        itemSlot(graphics, x, y, size, ITEM_CHECKER_CELL_SIZE, hovered);
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
        // The complete checker tile is scaled uniformly; the border remains one GUI pixel.
        if (width > 2 && height > 2) {
            drawItemGridTexture(graphics, x + 1, y + 1, width - 2, height - 2, cellSize);
        }
        stateOutline(graphics, x, y, width, height, selected, hovered, error);
    }

    /** Draws the standard item-grid background for the exact bounds. */
    public static void itemGrid(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) return;
        drawItemGridTexture(graphics, x, y, width, height, ITEM_CHECKER_CELL_SIZE);
    }

    /** Repeats compact slots with their preset gaps and outer padding. */
    public static void itemSelectorGrid(GuiGraphics graphics, int x, int y, int columns, int rows) {
        if (graphics == null || columns <= 0 || rows <= 0) return;
        ItemGridDensity density = ItemGridDensity.COMPACT;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                itemSlot(graphics, x + density.padding() + column * density.cellPitch(),
                        y + density.padding() + row * density.cellPitch(), density.slotSize(), false);
            }
        }
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
        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, x + offset, y + offset, 0);
        VanillaGuiDraw.scale(graphics, scale, scale, 1f);
        // Inside an item batch (grids) the icon is collected and drawn with the others.
        if (!dev.xyat.kineticcore.internal.client.gui.render.ItemBatch.queue(graphics, stack, decorations && font != null)) {
            KineticRenderRuntime.enableDepthTest();
            VanillaGuiDraw.item(graphics, stack, 0, 0);
            if (decorations && font != null) VanillaGuiDraw.itemDecorations(graphics, font, stack, 0, 0);
            KineticRenderRuntime.disableDepthTest();
        }
        VanillaGuiDraw.pop(graphics);
    }

    private static void drawItemGridTexture(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int cellSize
    ) {
        // Draw only complete square tiles. Padding absorbs any remainder, so neither
        // a non-square region nor its final row/column distorts or crops the texture.
        int tileSize = Math.min(width, height);
        if (tileSize < ITEM_SLOT_TEXTURE_SIZE) return;
        int columns = width / tileSize;
        int rows = height / tileSize;
        int left = x + (width - columns * tileSize) / 2;
        int top = y + (height - rows * tileSize) / 2;
        float scale = tileSize / (float) ITEM_SLOT_TEXTURE_SIZE;
        VanillaGuiDraw.push(graphics);
        try {
            VanillaGuiDraw.translate(graphics, left, top, 0);
            VanillaGuiDraw.scale(graphics, scale, scale, 1f);
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    VanillaGuiDraw.texture(graphics, ITEM_SLOT_TEXTURE,
                            column * ITEM_SLOT_TEXTURE_SIZE, row * ITEM_SLOT_TEXTURE_SIZE, 0, 0,
                            ITEM_SLOT_TEXTURE_SIZE, ITEM_SLOT_TEXTURE_SIZE,
                            ITEM_SLOT_TEXTURE_SIZE, ITEM_SLOT_TEXTURE_SIZE);
                }
            }
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
    }
}
