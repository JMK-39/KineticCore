package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Base container screen for Kinetic interfaces using the fixed 640x360 virtual canvas.
 * <p>
 * The shared control, overlay, focus and draft API comes from {@link KineticScreenHost}; this class only adapts the
 * vanilla container screen (slots, hovered-slot tooltips, container tick) to the scaled canvas.
 */
public abstract class KineticContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements KineticScreenHost {
    private final KineticScreenRuntime runtime = new KineticScreenRuntime(
            this,
            KineticCanvasTransform.canvas(1, 1),
            () -> font,
            () -> minecraft,
            this::addRenderableWidget,
            this::addWidget,
            this::removeWidget
    );

    /** Creates a new {@code KineticContainerScreen}. */
    protected KineticContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public final Screen screen() {
        return this;
    }

    @Override
    public final KineticScreenRuntime kineticRuntime() {
        return runtime;
    }

    @Override
    public final int pageWidth() {
        return uiWidth();
    }

    @Override
    public final int pageHeight() {
        return uiHeight();
    }

    /** Returns ui width. */
    public final int uiWidth() {
        return runtime.canvas().width();
    }

    /** Returns ui height. */
    public final int uiHeight() {
        return runtime.canvas().height();
    }

    /** Returns ui scale. */
    public final float uiScale() {
        return runtime.canvas().scale();
    }

    /** Reports whether screen-space coordinates are inside the fixed container UI canvas. */
    public final boolean isInsideUi(double screenX, double screenY) {
        return runtime.canvas().contains(screenX, screenY);
    }

    /** Requests the tooltip of the hovered registered control; returns whether one was shown. */
    protected final boolean requestWidgetTooltip(double mouseX, double mouseY) {
        return runtime.requestWidgetTooltip(mouseX, mouseY, widget -> true);
    }

    // ---- lifecycle ---------------------------------------------------------------------------------------------

    @Override
    protected final void init() {
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth();
        this.height = uiHeight();
        try {
            super.init();
            runtime.controls().clear();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    /** Builds this container screen's API-managed controls for the current UI rebuild. */
    protected abstract void buildUi();

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    @Override
    public final void rebuildUi() {
        clearControlFocus();
        closeContextMenu();
        updateMetrics();
        int screenWidth = this.width;
        int screenHeight = this.height;
        this.width = uiWidth();
        this.height = uiHeight();
        try {
            clearWidgets();
            runtime.controls().clear();
            super.init();
            buildUi();
        } finally {
            this.width = screenWidth;
            this.height = screenHeight;
        }
    }

    private void updateMetrics() {
        runtime.canvas().fit(
                width,
                height,
                KineticScreen.STANDARD_CANVAS_WIDTH,
                KineticScreen.STANDARD_CANVAS_HEIGHT,
                KineticScreen.STANDARD_SAFE_MARGIN
        );
    }

    /** {@inheritDoc} */
    @Override
    protected final void containerTick() {
        super.containerTick();
        runtime.tick(this::containerUiTick);
    }

    /** Handles business per-tick work after standard registered controls have ticked. */
    protected void containerUiTick() {
    }

    /** Runs business-specific cleanup when this Screen is removed. */
    protected void screenRemoved() {
    }

    /** Guarantees vanilla removal cleanup while allowing business cleanup through {@link #screenRemoved()}. */
    @Override
    public final void removed() {
        try {
            screenRemoved();
        } finally {
            super.removed();
        }
    }

    /**
     * Handles a business-specific close request before the standard parent navigation runs.
     * Return {@code true} when the request was fully handled or intentionally deferred; return {@code false} to continue with {@link #navigateBack()}.
     */
    protected boolean handleCloseRequest() {
        return false;
    }

    /** Routes every close request through the Kinetic close hook before standard parent navigation. */
    @Override
    public final void onClose() {
        if (!handleCloseRequest()) {
            navigateBack();
        }
    }

    // ---- rendering ---------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.render(graphics, mouseX, mouseY, partialTick, this::renderContainerFrame);
    }

    private void renderContainerFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.renderInCanvas(graphics, mouseX, mouseY, partialTick, (uiGraphics, virtualMouseX, virtualMouseY, tick) -> {
            super.render(uiGraphics, virtualMouseX, virtualMouseY, tick);
            renderUiForeground(uiGraphics, virtualMouseX, virtualMouseY, tick);
            runtime.controls().renderAutoCompleteSuggestions(uiGraphics, virtualMouseX, virtualMouseY);
            if (runtime.businessTooltipsAllowed(isInsideUi(mouseX, mouseY))) {
                requestContainerTooltips(uiGraphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
            }
        });
        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));
        renderScreenOverlay(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY, partialTick);
    }

    /** Requests container tooltips. */
    protected void requestContainerTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
        if (requestWidgetTooltip(virtualMouseX, virtualMouseY)) return;
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            showItemTooltip(hoveredSlot.getItem());
        }
    }

    /** Renders ui foreground. */
    protected void renderUiForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** Renders screen overlay. */
    protected void renderScreenOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY,
            float partialTick
    ) {
    }

    // ---- input -------------------------------------------------------------------------------------------------

    /** Handles business mouse clicks in virtual canvas coordinates after Overlay and autocomplete routing. */
    protected boolean containerMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Handles business mouse dragging in virtual canvas coordinates after Overlay and autocomplete routing. */
    protected boolean containerMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return false;
    }

    /** Handles business mouse releases in virtual canvas coordinates after Overlay and autocomplete routing. */
    protected boolean containerMouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Handles business mouse-wheel input in virtual canvas coordinates after Overlay and autocomplete routing. */
    protected boolean containerMouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /** Handles business mouse movement in virtual canvas coordinates after Overlay routing. */
    protected void containerMouseMoved(double mouseX, double mouseY) {
    }

    /** Handles business key presses after Overlay and autocomplete routing, before default Escape navigation. */
    protected boolean containerKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business key releases after Overlay routing, before registered controls. */
    protected boolean containerKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business character input after Overlay routing, before registered controls. */
    protected boolean containerCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /** Page-host hook: a press no registered control consumed. */
    protected boolean afterMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Page-host hook: a release no registered control consumed. */
    protected boolean afterMouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Page-host hook: a drag no registered control consumed. */
    protected boolean afterMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    /** Page-host hook: wheel input no registered control consumed. */
    protected boolean afterMouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return runtime.mouseClicked(mouseX, mouseY, button, (x, y, pressed) ->
                containerMouseClicked(x, y, pressed)
                        || super.mouseClicked(x, y, pressed)
                        || afterMouseClicked(x, y, pressed));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return runtime.mouseReleased(mouseX, mouseY, button, (x, y, released) ->
                containerMouseReleased(x, y, released)
                        || super.mouseReleased(x, y, released)
                        || afterMouseReleased(x, y, released));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return runtime.mouseDragged(mouseX, mouseY, button, dragX, dragY, (x, y, dragged, dx, dy) ->
                containerMouseDragged(x, y, dragged, dx, dy)
                        || super.mouseDragged(x, y, dragged, dx, dy)
                        || afterMouseDragged(x, y, dragged, dx, dy));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return runtime.mouseScrolled(mouseX, mouseY, delta, (x, y, wheel) ->
                containerMouseScrolled(x, y, wheel)
                        || GuiSessionRuntime.routeSelectionListWheel(children(), x, y, wheel)
                        || super.mouseScrolled(x, y, wheel)
                        || afterMouseScrolled(x, y, wheel));
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        runtime.mouseMoved(mouseX, mouseY, (x, y) -> {
            containerMouseMoved(x, y);
            super.mouseMoved(x, y);
        });
    }

    /** {@inheritDoc} */
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return runtime.keyPressed(keyCode, scanCode, modifiers, this::containerKeyPressed, super::keyPressed);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return runtime.keyReleased(keyCode, scanCode, modifiers,
                (key, scan, mods) -> containerKeyReleased(key, scan, mods) || super.keyReleased(key, scan, mods));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return runtime.charTyped(codePoint, modifiers,
                (character, mods) -> containerCharTyped(character, mods) || super.charTyped(character, mods));
    }
}
