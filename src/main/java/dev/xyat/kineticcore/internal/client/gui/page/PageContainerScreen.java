package dev.xyat.kineticcore.internal.client.gui.page;

import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.api.client.gui.page.KineticContainerPage;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticContainerScreen;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import javax.annotation.Nonnull;

/** Hosts a {@link KineticContainerPage} in the internal Kinetic container screen. */
public final class PageContainerScreen<M extends AbstractContainerMenu> extends KineticContainerScreen<M>
        implements ContainerPageHost {
    private final KineticContainerPage<M> page;
    private final PageBridge.Accessor access = PageBridge.access();
    private final PageUi ui = new PageUi(this);

    public PageContainerScreen(KineticContainerPage<M> page, Inventory inventory, Component title) {
        super(page.menu(), inventory, title);
        this.page = page;
        applyLayout();
        access.attach(page, this);
    }

    private void applyLayout() {
        int[] layout = access.containerLayout(page);
        this.imageWidth = layout[0];
        this.imageHeight = layout[1];
        this.titleLabelX = layout[2];
        this.titleLabelY = layout[3];
        this.inventoryLabelX = layout[4];
        this.inventoryLabelY = layout[5];
    }

    @Override
    protected void buildUi() {
        ui.reset();
        access.build(page, ui);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        access.renderContainerBackground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderUiForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        access.renderForeground(page, GuiGraphicsAdapter.wrap(graphics), mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderScreenOverlay(GuiGraphics graphics, int virtualMouseX, int virtualMouseY, int screenMouseX,
                                       int screenMouseY, float partialTick) {
        access.renderScreenOverlay(page, GuiGraphicsAdapter.wrap(graphics), screenMouseX, screenMouseY, partialTick);
    }

    @Override
    protected void requestContainerTooltips(GuiGraphics graphics, int virtualMouseX, int virtualMouseY, int screenMouseX,
                                            int screenMouseY) {
        if (requestWidgetTooltip(virtualMouseX, virtualMouseY)) return;
        access.renderTooltips(page, virtualMouseX, virtualMouseY);
    }

    @Override
    protected void containerUiTick() {
        ui.tickCustomControls();
        access.onTick(page);
    }

    @Override
    protected boolean containerMouseClicked(double mouseX, double mouseY, int button) {
        return access.onMouseClickCapture(page, InputRecords.mouse(mouseX, mouseY, button));
    }

    @Override
    protected boolean afterMouseClicked(double mouseX, double mouseY, int button) {
        MouseInput input = InputRecords.mouse(mouseX, mouseY, button);
        return access.onMouseClick(page, input);
    }

    @Override
    protected boolean containerMouseReleased(double mouseX, double mouseY, int button) {
        // 原版容器界面总会消费松开/拖动，页面钩子必须先于原版执行；松开同时继续交给原版。
        // Vanilla container screens always consume release/drag, so the page hooks run first; releases still
        // continue to vanilla afterwards.
        access.onMouseRelease(page, InputRecords.mouse(mouseX, mouseY, button));
        ui.releasePressedCustomControls(mouseX, mouseY, button);
        return false;
    }

    @Override
    protected boolean containerMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return access.onMouseDrag(page, InputRecords.drag(mouseX, mouseY, button, dragX, dragY));
    }

    @Override
    protected boolean afterMouseScrolled(double mouseX, double mouseY, double delta) {
        return access.onMouseScroll(page, InputRecords.scroll(mouseX, mouseY, delta));
    }

    @Override
    protected void containerMouseMoved(double mouseX, double mouseY) {
        access.onMouseMove(page, mouseX, mouseY);
    }

    @Override
    protected boolean containerKeyPressed(int keyCode, int scanCode, int modifiers) {
        return access.onKeyPress(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean containerKeyReleased(int keyCode, int scanCode, int modifiers) {
        return access.onKeyRelease(page, InputRecords.key(keyCode, scanCode, modifiers));
    }

    @Override
    protected boolean containerCharTyped(char codePoint, int modifiers) {
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
        return KineticLayout.measure(uiWidth(), uiHeight(), uiWidth(), uiHeight());
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
        // Container pages route ctrl+wheel through their own onMouseScroll hook.
    }

    @Override
    public boolean isOpen() {
        return KineticClientRuntimeImpl.currentScreen() == this;
    }

    @Override
    public Slot hoveredSlot() {
        return hoveredSlot;
    }

    @Override
    public int leftPos() {
        return leftPos;
    }

    @Override
    public int topPos() {
        return topPos;
    }

    @Override
    public int imageWidth() {
        return imageWidth;
    }

    @Override
    public int imageHeight() {
        return imageHeight;
    }

    @Override
    public void setImageSize(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }

    @Override
    public void setTitleLabelPosition(int x, int y) {
        this.titleLabelX = x;
        this.titleLabelY = y;
    }

    @Override
    public void setInventoryLabelPosition(int x, int y) {
        this.inventoryLabelX = x;
        this.inventoryLabelY = y;
    }
}
