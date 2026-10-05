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
    //? if <26.1
    @Override
    public final void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.render(graphics, mouseX, mouseY, partialTick, this::renderContainerFrame);
    }

    //? if >=1.20.2 <26.1 {
    /*// Screen.render draws the background itself since 1.20.2; KineticScreenRuntime already drew it this frame.
    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }
    *///?}
    //? if >=26.1 {
    /*// 26.1 draws the screen background before and outside extractRenderState, in screen space, and
    // KineticScreenRuntime already drew the dimmed background this frame. The container background belongs to the
    // canvas with the slots, so renderContainerFrame draws it there.
    @Override
    public void extractBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
    }

    // AbstractContainerScreen no longer declares renderBg on 26.1; subclasses still draw their background in it.
    protected abstract void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY);
    *///?}

    private void renderContainerFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.renderInCanvas(graphics, mouseX, mouseY, partialTick, (uiGraphics, virtualMouseX, virtualMouseY, tick) -> {
            //? if >=26.1
            /*renderBg(uiGraphics, tick, virtualMouseX, virtualMouseY);*/
            vanillaRender(uiGraphics, virtualMouseX, virtualMouseY, tick);
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
    //? if <26.1
    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return runtime.mouseClicked(mouseX, mouseY, button, (x, y, pressed) ->
                containerMouseClicked(x, y, pressed)
                        || vanillaMouseClicked(x, y, pressed)
                        || afterMouseClicked(x, y, pressed));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return runtime.mouseReleased(mouseX, mouseY, button, (x, y, released) ->
                containerMouseReleased(x, y, released)
                        || vanillaMouseReleased(x, y, released)
                        || afterMouseReleased(x, y, released));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return runtime.mouseDragged(mouseX, mouseY, button, dragX, dragY, (x, y, dragged, dx, dy) ->
                containerMouseDragged(x, y, dragged, dx, dy)
                        || vanillaMouseDragged(x, y, dragged, dx, dy)
                        || afterMouseDragged(x, y, dragged, dx, dy));
    }

    /** {@inheritDoc} */
    //? if >=1.20.2 {
    /*@Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
    *///?} else {
    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    //?}
        return runtime.mouseScrolled(mouseX, mouseY, delta, (x, y, wheel) ->
                containerMouseScrolled(x, y, wheel)
                        || GuiSessionRuntime.routeSelectionListWheel(children(), x, y, wheel)
                        //? if >=1.20.2 {
                        /*|| super.mouseScrolled(x, y, scrollX, wheel)
                        *///?} else {
                        || super.mouseScrolled(x, y, wheel)
                        //?}
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
    //? if <26.1
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return runtime.keyPressed(keyCode, scanCode, modifiers, this::containerKeyPressed, this::vanillaKeyPressed);
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return runtime.keyReleased(keyCode, scanCode, modifiers,
                (key, scan, mods) -> containerKeyReleased(key, scan, mods) || vanillaKeyReleased(key, scan, mods));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return runtime.charTyped(codePoint, modifiers,
                (character, mods) -> containerCharTyped(character, mods) || vanillaCharTyped(character, mods));
    }

    // ---- vanilla input and rendering across versions -----------------------------------------------------------

    //? if >=26.1 {
    /*// Since 1.21.9 input arrives as events. The coordinate handlers above stay the entry points; the mouse event that
    // started the current call is kept, so vanilla handling gets its modifiers and double click back.
    private net.minecraft.client.input.MouseButtonEvent currentMouseEvent;
    private boolean currentDoubleClick;

    @Override
    public final void extractRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public final boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        currentMouseEvent = event;
        currentDoubleClick = doubleClick;
        return mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        currentMouseEvent = event;
        return mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public final boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        currentMouseEvent = event;
        return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public final boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public final boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        boolean handled = false;
        for (char character : Character.toChars(event.codepoint())) handled |= charTyped(character, 0);
        return handled;
    }

    private net.minecraft.client.input.MouseButtonEvent mouseEventAt(double x, double y, int button) {
        int modifiers = currentMouseEvent == null ? 0 : currentMouseEvent.modifiers();
        return new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, modifiers));
    }

    private boolean vanillaMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseEventAt(mouseX, mouseY, button), currentDoubleClick);
    }

    private boolean vanillaMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseEventAt(mouseX, mouseY, button));
    }

    private boolean vanillaMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseEventAt(mouseX, mouseY, button), dragX, dragY);
    }

    private boolean vanillaKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
    }

    private boolean vanillaKeyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
    }

    private boolean vanillaCharTyped(char codePoint, int modifiers) {
        return super.charTyped(new net.minecraft.client.input.CharacterEvent(codePoint));
    }

    private void vanillaRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    *///?} else {
    private boolean vanillaMouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean vanillaMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean vanillaMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean vanillaKeyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean vanillaKeyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean vanillaCharTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    private void vanillaRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    //?}
}
