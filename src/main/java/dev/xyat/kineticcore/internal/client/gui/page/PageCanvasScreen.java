package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import net.minecraft.client.gui.GuiGraphics;

/** Hosts a canvas-layout {@link KineticPage} in the internal Kinetic canvas screen. */
final class PageCanvasScreen extends KineticScreen implements PageHost {
    private final KineticPage page;
    private final PageBridge.Accessor access = PageBridge.access();
    private final PageUi ui = new PageUi(this);

    PageCanvasScreen(KineticPage page) {
        super(page.title());
        this.page = page;
        useCanvas(access.canvasDesignWidth(page), access.canvasDesignHeight(page), access.canvasSafeMargin(page));
        access.attach(page, this);
    }

    @Override
    protected void buildUi() {
        ui.reset();
        access.build(page, ui);
    }

    @Override
    protected void renderCanvasBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        access.renderBackground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderCanvasForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        access.renderForeground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int virtualMouseX, int virtualMouseY, int screenMouseX, int screenMouseY) {
        access.renderTooltips(page, virtualMouseX, virtualMouseY);
    }

    @Override
    protected void canvasTick() {
        ui.tickCustomControls();
        access.onTick(page);
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        MouseInput input = InputRecords.mouse(mouseX, mouseY, button);
        if (access.onMouseClickCapture(page, input)) return true;
        if (super.canvasMouseClicked(mouseX, mouseY, button)) return true;
        return access.onMouseClick(page, input);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        // 松开总是通知页面与控件，避免拖动在控件上松开时丢失 / Releases always reach both the page and the controls.
        boolean pageHandled = access.onMouseRelease(page, InputRecords.mouse(mouseX, mouseY, button));
        boolean controlHandled = super.canvasMouseReleased(mouseX, mouseY, button);
        ui.releasePressedCustomControls(mouseX, mouseY, button);
        return pageHandled || controlHandled;
    }

    @Override
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (access.onMouseDrag(page, InputRecords.drag(mouseX, mouseY, button, dragX, dragY))) return true;
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (super.canvasMouseScrolled(mouseX, mouseY, delta)) return true;
        return access.onMouseScroll(page, InputRecords.scroll(mouseX, mouseY, delta));
    }

    @Override
    protected void canvasMouseMoved(double mouseX, double mouseY) {
        access.onMouseMove(page, mouseX, mouseY);
    }

    @Override
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        return access.onKeyPress(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean canvasKeyReleased(int keyCode, int scanCode, int modifiers) {
        return access.onKeyRelease(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean canvasCharTyped(char codePoint, int modifiers) {
        return access.onCharTyped(page, InputRecords.character(codePoint, modifiers));
    }

    @Override
    protected boolean handleCloseRequest() {
        return access.onCloseRequested(page);
    }

    @Override
    protected void screenRemoved() {
        access.onRemoved(page);
    }

    @Override
    public boolean isPauseScreen() {
        return access.pausesGame(page);
    }

    @Override
    public KineticPage page() {
        return page;
    }

    @Override
    public KineticScreenHost screenHost() {
        return this;
    }

    @Override
    public KineticUi ui() {
        return ui;
    }

    @Override
    public KineticLayout.Metrics metrics() {
        return layout();
    }

    @Override
    public void rebuild() {
        rebuildUi();
    }

    @Override
    public void close() {
        onClose();
    }

    @Override
    public void openChild(KineticPage child) {
        PageScreens.openChild(child);
    }

    @Override
    public boolean isOpen() {
        return KineticClientRuntimeImpl.currentScreen() == this;
    }
}
