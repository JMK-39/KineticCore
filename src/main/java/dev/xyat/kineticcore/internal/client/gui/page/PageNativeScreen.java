package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticNativeScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;

/** Hosts a native-layout {@link KineticPage} in the internal native Kinetic screen. */
final class PageNativeScreen extends KineticNativeScreen implements PageHost {
    private final KineticPage page;
    private final PageBridge.Accessor access = PageBridge.access();
    private final PageUi ui = new PageUi(this);

    PageNativeScreen(KineticPage page) {
        super(page.title());
        this.page = page;
        access.attach(page, this);
    }

    @Override
    protected void buildUi() {
        ui.reset();
        access.build(page, ui);
    }

    @Override
    protected void renderNativeBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        access.renderBackground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderNativeOverlayRequests(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        access.renderForeground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderNativeTooltips(int mouseX, int mouseY) {
        access.renderTooltips(page, mouseX, mouseY);
    }

    @Override
    protected void nativeTick() {
        ui.tickCustomControls();
        access.onTick(page);
    }

    @Override
    protected boolean nativeMouseClicked(double mouseX, double mouseY, int button) {
        return access.onMouseClickCapture(page, InputRecords.mouse(mouseX, mouseY, button));
    }

    @Override
    protected boolean afterMouseClicked(double mouseX, double mouseY, int button) {
        MouseInput input = InputRecords.mouse(mouseX, mouseY, button);
        return access.onMouseClick(page, input);
    }

    @Override
    protected boolean nativeMouseReleased(double mouseX, double mouseY, int button) {
        // 松开总是通知页面，再继续交给控件 / Releases always reach the page, then continue to the controls.
        access.onMouseRelease(page, InputRecords.mouse(mouseX, mouseY, button));
        ui.releasePressedCustomControls(mouseX, mouseY, button);
        return false;
    }

    @Override
    protected boolean nativeMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return access.onMouseDrag(page, InputRecords.drag(mouseX, mouseY, button, dragX, dragY));
    }

    @Override
    protected boolean afterMouseScrolled(double mouseX, double mouseY, double delta) {
        return access.onMouseScroll(page, InputRecords.scroll(mouseX, mouseY, delta));
    }

    @Override
    protected void nativeMouseMoved(double mouseX, double mouseY) {
        access.onMouseMove(page, mouseX, mouseY);
    }

    @Override
    protected boolean nativeKeyPressed(int keyCode, int scanCode, int modifiers) {
        return access.onKeyPress(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean nativeKeyReleased(int keyCode, int scanCode, int modifiers) {
        return access.onKeyRelease(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean nativeCharTyped(char codePoint, int modifiers) {
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
        return KineticLayout.measure(Math.max(1, width), Math.max(1, height), Math.max(1, width), Math.max(1, height));
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
    public void registerPreviewWheelTarget(EntityPreviewRenderer renderer, String key, int x, int y, int width, int height) {
        // Native pages route ctrl+wheel through their own onMouseScroll hook.
    }

    @Override
    public boolean isOpen() {
        return KineticClientRuntimeImpl.currentScreen() == this;
    }
}
