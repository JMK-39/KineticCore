package dev.xyat.kineticcore.api.client.gui.theme;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Kinetic 主题：统一的颜色调色板与标准绘制助手。附属不应硬编码颜色，而应使用这里的表面、状态描边与指示色。
 * The Kinetic theme: the shared palette and standard drawing helpers. Addons should use these surfaces, state
 * outlines and indicator colors instead of hard-coded colors.
 */
public final class KineticTheme {
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
            0xFF8B8B8B,
            0xFFFFFFFF,
            0xFFB0B0B0,
            0xFF55FF55,
            0xFFE05A5A,
            0xFF171717,
            0xFFFF9800,
            0xFFFFD700,
            0xC0000000
    );

    private KineticTheme() {
    }

    /** Returns the active immutable Kinetic palette for theme-consistent custom rendering. */
    public static Palette current() {
        return GuiTheme.current();
    }

    /** Returns the standard cyan text color used on the light input surface. */
    public static int fieldText() {
        return GuiTheme.fieldText();
    }

    /** Returns the standard cyan read-only text color used on the light input surface. */
    public static int fieldMutedText() {
        return GuiTheme.fieldMutedText();
    }

    /** 输入框占位提示颜色（纯白，仅空且无焦点时显示）/ Input placeholder color (pure white; empty and unfocused only). */
    public static int fieldPlaceholderText() {
        return GuiTheme.fieldPlaceholderText();
    }

    /** 输入框内容等于默认值时的颜色（青色）/ Input text color when the value equals its default (cyan). */
    public static int fieldDefaultText() {
        return GuiTheme.fieldDefaultText();
    }

    /** 输入框内容已修改时的颜色（绿）/ Input text color when the value was modified (green). */
    public static int fieldModifiedText() {
        return GuiTheme.fieldModifiedText();
    }

    /** Input text color for invalid values or insufficient quantities (red). */
    public static int fieldErrorText() {
        return GuiTheme.fieldErrorText();
    }

    /** Draws the standard full-canvas background using the active theme. */
    public static void canvasBackground(KineticGraphics graphics, int width, int height) {
        GuiTheme.canvasBackground(GuiGraphicsAdapter.unwrap(graphics), width, height);
    }

    /** Draws one standard themed surface without adding an outline. */
    public static void surface(KineticGraphics graphics, int x, int y, int width, int height, Surface surface) {
        GuiTheme.surface(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, surface);
    }

    /** Draws one themed surface with an explicit alpha multiplier. */
    public static void surface(KineticGraphics graphics, int x, int y, int width, int height, Surface surface, float alpha) {
        GuiTheme.surface(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, surface, alpha);
    }

    /** Draws a standard themed surface and applies selected, hovered, or error outline priority. */
    public static void stateSurface(KineticGraphics graphics, int x, int y, int width, int height, Surface surface, boolean selected, boolean hovered, boolean error) {
        GuiTheme.stateSurface(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, surface, selected, hovered, error);
    }

    /** 中键跳转后选中项的反色闪烁（白底黑框），所有列表/网格/Tab 统一走这里。 Inverse flash drawn over the selected row/cell/tab after a middle-click jump. */
    public static void selectionFlash(KineticGraphics graphics, int x, int y, int width, int height) {
        GuiTheme.selectionFlash(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** 反色闪烁期间文字颜色 / Text color to use on top of {@link #selectionFlash}. */
    public static int selectionFlashText() {
        return GuiTheme.selectionFlashText();
    }

    /** Draws a standard theme checkerboard using the active panel and alternate-panel colors. */
    public static void checkerboard(KineticGraphics graphics, int x, int y, int width, int height, int cellSize) {
        GuiTheme.checkerboard(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, cellSize);
    }

    /** Draws a generic RGB color swatch without exposing widget implementation details. */
    public static void colorSwatch(KineticGraphics graphics, int x, int y, int width, int height, int rgb, boolean outlined) {
        GuiTheme.colorSwatch(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, rgb, outlined);
    }

    /** Draws a one-pixel horizontal separator using the active theme border color. */
    public static void separator(KineticGraphics graphics, int x, int y, int width) {
        GuiTheme.separator(GuiGraphicsAdapter.unwrap(graphics), x, y, width);
    }

    /** Draws a one-pixel vertical separator using the active theme border color. */
    public static void verticalSeparator(KineticGraphics graphics, int x, int y, int height) {
        GuiTheme.verticalSeparator(GuiGraphicsAdapter.unwrap(graphics), x, y, height);
    }

    /** Draws the standard outer frame used by item and rule grids without adding per-cell backgrounds. */
    public static void gridFrame(KineticGraphics graphics, int x, int y, int width, int height) {
        GuiTheme.gridFrame(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** Draws the standard selected, hovered, or error outline for a control. */
    public static void stateOutline(KineticGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered, boolean error) {
        GuiTheme.stateOutline(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, selected, hovered, error);
    }

    /** Draws a standard state outline with a theme-owned color and caller-selected thickness. */
    public static void stateOutline(KineticGraphics graphics, int x, int y, int width, int height, boolean selected, boolean hovered, boolean error, int thickness) {
        GuiTheme.stateOutline(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, selected, hovered, error, thickness);
    }

    /** Draws one standard semantic indicator outline without exposing raw border colors to business code. */
    public static void indicatorOutline(KineticGraphics graphics, int x, int y, int width, int height, Indicator indicator) {
        GuiTheme.indicatorOutline(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, indicator);
    }

    /** Draws a semantic indicator outline with a caller-selected thickness. */
    public static void indicatorOutline(KineticGraphics graphics, int x, int y, int width, int height, Indicator indicator, int thickness) {
        GuiTheme.indicatorOutline(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, indicator, thickness);
    }

    /** Fills one generic semantic indicator rectangle using the active Kinetic theme. */
    public static void indicatorFill(KineticGraphics graphics, int x, int y, int width, int height, Indicator indicator) {
        GuiTheme.indicatorFill(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, indicator);
    }

    /** Fills one generic semantic indicator rectangle using an explicit alpha multiplier. */
    public static void indicatorFill(KineticGraphics graphics, int x, int y, int width, int height, Indicator indicator, float alpha) {
        GuiTheme.indicatorFill(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, indicator, alpha);
    }

    /** Returns the active theme color for a semantic indicator. */
    public static int indicatorColor(Indicator indicator) {
        return GuiTheme.indicatorColor(indicator);
    }

    /** Draws a panel using the alternate panel background and standard border. */
    public static void panelAlt(KineticGraphics graphics, int x, int y, int width, int height) {
        GuiTheme.panelAlt(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** Draws the standard Kinetic panel using the active theme's panel background and border. */
    public static void panel(KineticGraphics graphics, int x, int y, int width, int height) {
        GuiTheme.panel(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** Draws the standard full-surface Kinetic shadow. */
    public static void shadow(KineticGraphics graphics, int width, int height) {
        GuiTheme.shadow(GuiGraphicsAdapter.unwrap(graphics), width, height);
    }

    /**
     * Returns a copy of {@code text} in the theme's muted text color, for secondary tooltip and detail lines.
     * Formatting codes inside the text still take precedence.
     */
    public static MutableComponent muted(Component text) {
        return text.copy().withStyle(style -> style.withColor(current().mutedText() & 0xFFFFFF));
    }

    /** Draws theme text with the requested opacity while the API owns blend-state handling. */
    public static void alphaText(KineticGraphics graphics, Component text, int x, int y, float alpha) {
        GuiTheme.alphaText(GuiGraphicsAdapter.unwrap(graphics), KineticClientRuntimeImpl.font(), text, x, y, alpha);
    }

    /** Returns whether the supplied GUI-space point lies inside the rectangular bounds. */
    public static boolean hovering(double mouseX, double mouseY, int x, int y, int width, int height) {
        return GuiTheme.hovering(mouseX, mouseY, x, y, width, height);
    }

    /** Draws one standard 18x18 item slot in its normal state. */
    public static void itemSlot(KineticGraphics graphics, int x, int y) {
        GuiTheme.itemSlot(GuiGraphicsAdapter.unwrap(graphics), x, y);
    }

    /** Draws a standard-size themed item slot. */
    public static void itemSlot(KineticGraphics graphics, int x, int y, boolean hovered) {
        GuiTheme.itemSlot(GuiGraphicsAdapter.unwrap(graphics), x, y, hovered);
    }

    /** Draws a square item slot using the standard checker cell size. */
    public static void itemSlot(KineticGraphics graphics, int x, int y, int size, boolean hovered) {
        GuiTheme.itemSlot(GuiGraphicsAdapter.unwrap(graphics), x, y, size, hovered);
    }

    /** Draws a themed item slot with explicit slot and cell sizes. */
    public static void itemSlot(KineticGraphics graphics, int x, int y, int size, int cellSize, boolean hovered) {
        GuiTheme.itemSlot(GuiGraphicsAdapter.unwrap(graphics), x, y, size, cellSize, hovered);
    }

    /** Draws one item slot with explicit dimensions and state flags. */
    public static void itemSlot(KineticGraphics graphics, int x, int y, int width, int height, int cellSize, boolean selected, boolean hovered, boolean error) {
        GuiTheme.itemSlot(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, cellSize, selected, hovered, error);
    }

    /** Draws the standard item-grid background for the exact bounds. */
    public static void itemGrid(KineticGraphics graphics, int x, int y, int width, int height) {
        GuiTheme.itemGrid(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** Draws an item-selector grid with explicit column and row counts. */
    public static void itemSelectorGrid(KineticGraphics graphics, int x, int y, int columns, int rows) {
        GuiTheme.itemSelectorGrid(GuiGraphicsAdapter.unwrap(graphics), x, y, columns, rows);
    }

    /** Renders an item stack using the standard Kinetic slot sizing and decoration rules. */
    public static void item(KineticGraphics graphics, ItemStack stack, int x, int y, int slotSize, float scale, boolean decorations) {
        GuiTheme.item(GuiGraphicsAdapter.unwrap(graphics), KineticClientRuntimeImpl.font(), stack, x, y, slotSize, scale, decorations);
    }

    /**
     * 在自绘行/自绘控件中绘制标准按钮外观（仅绘制，不处理点击）。
     * Draws the standard button look inside self-drawn rows or controls (paint only; clicks are yours).
     */
    public static void button(KineticGraphics graphics, int x, int y, int width, int height, Component text,
                              boolean hovered, boolean active, boolean error) {
        dev.xyat.kineticcore.internal.client.gui.widget.button.InlineButtonRenderer.render(
                GuiGraphicsAdapter.unwrap(graphics), x, y, width, height, text, hovered, active, error);
    }
}
