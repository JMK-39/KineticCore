package dev.xyat.kineticcore.api.client.gui.selector;

import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.page.PageLayout;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.selector.HudPositionEditor;
import net.minecraft.network.chat.Component;

/**
 * HUD 元素位置/缩放编辑页（原生坐标）：拖动移动、滚轮缩放、方向键微调，底部提供保存、重置、取消按钮。子类提供元素尺寸、
 * 初始与默认布局、预览绘制和保存逻辑。
 * HUD element position/scale editor page (native coordinates): drag to move, wheel to scale, arrow keys to nudge,
 * with save, reset and cancel buttons. Subclasses supply the element size, initial and default layouts, the preview
 * rendering and the save logic.
 */
public abstract class KineticHudEditorPage extends KineticPage {
    /** 默认最小缩放 / Default minimum scale. */
    public static final double DEFAULT_MINIMUM_SCALE = 0.5D;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_GAP = 6;

    /** 元素左上角（原生坐标）与缩放 / Element top-left in native coordinates and its scale. */
    public record HudLayout(int x, int y, double scale) {
    }

    private final HudPositionEditor editor = new HudPositionEditor();

    /** 创建编辑页（不暂停游戏）/ Creates the editor page (does not pause the game). */
    protected KineticHudEditorPage(Component title) {
        super(title, PageLayout.NATIVE);
        setPausesGame(false);
    }

    /** 未缩放元素宽度 / Unscaled element width. */
    protected abstract int elementWidth();

    /** 未缩放元素高度 / Unscaled element height. */
    protected abstract int elementHeight();

    /** 打开时的布局 / Layout when the editor opens. */
    protected abstract HudLayout initialLayout(int screenWidth, int screenHeight);

    /** 点击“重置”恢复的布局 / Layout restored by the reset button. */
    protected abstract HudLayout defaultLayout(int screenWidth, int screenHeight);

    /** 在 (x, y) 绘制未缩放的元素预览（缩放由编辑器处理）/ Draws the unscaled preview at (x, y); scaling is handled by the editor. */
    protected abstract void renderElement(KineticGraphics graphics, int x, int y, int mouseX, int mouseY);

    /** 保存当前布局 / Saves the current layout. */
    protected abstract void save(HudLayout layout);

    /**
     * 绘制原版背包参考图（含随鼠标转向的玩家模型），供自定义的背包内元素位置编辑页使用。
     * Draws the vanilla inventory reference (including the player model that follows the mouse), for custom pages
     * that position elements inside the inventory.
     *
     * @param pageWidth  页面宽度 / page width ({@code width()})
     * @param pageHeight 页面高度 / page height ({@code height()})
     */
    public static void renderInventoryReference(KineticGraphics graphics, int pageWidth, int pageHeight,
                                                int mouseX, int mouseY) {
        var player = dev.xyat.kineticcore.api.runtime.KineticClientRuntime.localPlayer();
        if (player == null) return;
        HudPositionEditor.renderInventoryReference(GuiGraphicsAdapter.unwrap(graphics),
                dev.xyat.kineticcore.api.runtime.KineticClientRuntime.font(), player, pageWidth, pageHeight,
                mouseX, mouseY);
    }

    /**
     * Draws behind the guides and the element, for example {@link #renderInventoryReference} for an element placed
     * inside the inventory. Draws nothing by default.
     */
    protected void renderBackdrop(KineticGraphics graphics, int mouseX, int mouseY) {
    }

    /** Whether the mouse wheel over the element changes its scale; {@code false} keeps the initial scale. */
    protected boolean scalable() {
        return true;
    }

    /** 最小缩放 / Minimum scale. */
    protected double minimumScale() {
        return DEFAULT_MINIMUM_SCALE;
    }

    /** 操作说明 / Instruction line. */
    protected Component instruction() {
        return KineticI18n.translatable("screen.kineticcore.hud_editor.instruction_scale");
    }

    /** 位置说明行 / Position description line. */
    protected Component positionText(HudLayout layout) {
        return KineticI18n.translatable(
                "screen.kineticcore.hud_editor.position_scale",
                Component.literal(String.valueOf(layout.x())),
                Component.literal(String.valueOf(layout.y())),
                Component.literal(String.valueOf(Math.round(layout.scale() * 100.0D)))
        );
    }

    /** 当前布局 / Current layout. */
    protected final HudLayout currentLayout() {
        return new HudLayout(editor.getX(), editor.getY(), editor.getScale());
    }

    /** 当前缩放后元素宽度 / Current scaled element width. */
    protected final int scaledElementWidth() {
        return editor.getElementWidth();
    }

    /** 当前缩放后元素高度 / Current scaled element height. */
    protected final int scaledElementHeight() {
        return editor.getElementHeight();
    }

    /** 恢复默认布局 / Restores the default layout. */
    protected final void resetLayout() {
        editor.reset();
    }

    @Override
    protected final void build(KineticUi ui) {
        HudLayout initial = initialLayout(width(), height());
        HudLayout defaults = defaultLayout(width(), height());
        editor.initialize(width(), height(), elementWidth(), elementHeight(), initial.x(), initial.y(),
                defaults.x(), defaults.y(), initial.scale(), defaults.scale(), minimumScale());
        int totalWidth = BUTTON_WIDTH * 3 + BUTTON_GAP * 2;
        int startX = (width() - totalWidth) / 2;
        int buttonY = height() - 30;
        ui.button(startX, buttonY, BUTTON_WIDTH)
                .text(KineticI18n.translatable("gui.kineticcore.hud_editor.save"))
                .onClick(() -> save(currentLayout()))
                .build();
        ui.button(startX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH)
                .text(KineticI18n.translatable("gui.kineticcore.hud_editor.reset"))
                .onClick(this::resetLayout)
                .build();
        ui.button(startX + (BUTTON_WIDTH + BUTTON_GAP) * 2, buttonY, BUTTON_WIDTH)
                .text(KineticI18n.translatable("gui.kineticcore.hud_editor.cancel"))
                .onClick(this::close)
                .build();
    }

    @Override
    protected final void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackdrop(graphics, mouseX, mouseY);
        editor.render(GuiGraphicsAdapter.unwrap(graphics), KineticClientRuntimeImpl.font(), mouseX, mouseY, title(),
                instruction(), positionText(currentLayout()),
                (vanilla, x, y, elementMouseX, elementMouseY) ->
                        renderElement(GuiGraphicsAdapter.wrap(vanilla), x, y, elementMouseX, elementMouseY));
    }

    @Override
    protected final boolean onMouseClickCapture(MouseInput input) {
        return editor.mouseClicked(input.x(), input.y(), input.rawButton());
    }

    @Override
    protected final boolean onMouseDrag(MouseDragInput input) {
        return editor.mouseDragged(input.x(), input.y(), input.rawButton());
    }

    @Override
    protected final boolean onMouseRelease(MouseInput input) {
        return editor.mouseReleased(input.rawButton());
    }

    @Override
    protected final boolean onMouseScroll(ScrollInput input) {
        return scalable() && editor.mouseScrolled(input.x(), input.y(), input.deltaY());
    }

    @Override
    protected final boolean onKeyPress(KeyInput input) {
        return editor.keyPressed(input.keyCode(), input.hasShift());
    }
}
