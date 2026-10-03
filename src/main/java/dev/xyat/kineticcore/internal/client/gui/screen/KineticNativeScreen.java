package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 使用 Minecraft 原生屏幕坐标的 Kinetic 编辑界面基类。
 * 适合不需要虚拟画布，但仍需要统一返回、草稿回滚和保存边界的界面。
 * <p>
 * The shared control, overlay, focus and draft API comes from {@link KineticScreenHost}; page coordinates are
 * screen coordinates here.
 */
public abstract class KineticNativeScreen extends Screen implements KineticScreenHost {
    private final KineticScreenRuntime runtime = new KineticScreenRuntime(
            this,
            KineticCanvasTransform.nativeCoordinates(),
            () -> font,
            () -> minecraft,
            this::addRenderableWidget,
            this::addWidget,
            this::removeWidget
    );

    /** Creates a new {@code KineticNativeScreen}. */
    protected KineticNativeScreen(Component title) {
        super(title);
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
        return width;
    }

    @Override
    public final int pageHeight() {
        return height;
    }

    // ---- lifecycle ---------------------------------------------------------------------------------------------

    @Override
    protected final void init() {
        super.init();
        rebuildUi();
    }

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    @Override
    public final void rebuildUi() {
        clearControlFocus();
        closeContextMenu();
        clearWidgets();
        runtime.controls().clear();
        buildUi();
    }

    /** Builds this native-coordinate screen's API-managed controls for the current UI rebuild. */
    protected abstract void buildUi();

    /** {@inheritDoc} */
    @Override
    public final void tick() {
        super.tick();
        runtime.tick(this::nativeTick);
    }

    /** Handles business per-tick work after standard registered controls have ticked. */
    protected void nativeTick() {
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
        runtime.render(graphics, mouseX, mouseY, partialTick, this::renderNativeFrame);
    }

    //? if >=1.20.2 <26.1 {
    /*// Screen.render draws the background itself since 1.20.2; KineticScreenRuntime already drew it this frame.
    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >=26.1 {
    /*// Screen.extractRenderState draws the background itself; KineticScreenRuntime already drew it this frame.
    @Override
    public void extractBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    private void renderNativeFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderNativeBackground(graphics, mouseX, mouseY, partialTick);
        vanillaRender(graphics, mouseX, mouseY, partialTick);
        renderNativeOverlayRequests(graphics, mouseX, mouseY, partialTick);
        runtime.controls().renderAutoCompleteSuggestions(graphics, mouseX, mouseY);
        if (runtime.businessTooltipsAllowed(true)
                && !runtime.requestWidgetTooltip(mouseX, mouseY, widget -> true)) {
            renderNativeTooltips(mouseX, mouseY);
        }
    }

    /** Renders business content behind registered controls in native screen coordinates. */
    protected void renderNativeBackground(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    /** Renders native overlay requests above registered controls. */
    protected void renderNativeOverlayRequests(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    }

    /** Requests business tooltips after control tooltips and the scrollbar hint were checked. */
    protected void renderNativeTooltips(int mouseX, int mouseY) {
    }

    // ---- input -------------------------------------------------------------------------------------------------

    /** Handles business mouse clicks after Overlay and autocomplete routing. */
    protected boolean nativeMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Handles business mouse dragging after Overlay and autocomplete routing. */
    protected boolean nativeMouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        return false;
    }

    /** Handles business mouse releases after Overlay and autocomplete routing. */
    protected boolean nativeMouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Handles business mouse-wheel input after Overlay and autocomplete routing. */
    protected boolean nativeMouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /** Handles business mouse movement after Overlay routing. */
    protected void nativeMouseMoved(double mouseX, double mouseY) {
    }

    /** Handles business key presses after Overlay and autocomplete routing, before default Escape navigation. */
    protected boolean nativeKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business key releases after Overlay routing, before registered controls. */
    protected boolean nativeKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business character input after Overlay routing, before registered controls. */
    protected boolean nativeCharTyped(char codePoint, int modifiers) {
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
                nativeMouseClicked(x, y, pressed)
                        || vanillaMouseClicked(x, y, pressed)
                        || afterMouseClicked(x, y, pressed));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return runtime.mouseReleased(mouseX, mouseY, button, (x, y, released) ->
                nativeMouseReleased(x, y, released)
                        || vanillaMouseReleased(x, y, released)
                        || afterMouseReleased(x, y, released));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return runtime.mouseDragged(mouseX, mouseY, button, dragX, dragY, (x, y, dragged, dx, dy) ->
                nativeMouseDragged(x, y, dragged, dx, dy)
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
                nativeMouseScrolled(x, y, wheel)
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
            nativeMouseMoved(x, y);
            super.mouseMoved(x, y);
        });
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return runtime.keyPressed(keyCode, scanCode, modifiers, this::nativeKeyPressed, this::vanillaKeyPressed);
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return runtime.keyReleased(keyCode, scanCode, modifiers,
                (key, scan, mods) -> nativeKeyReleased(key, scan, mods) || vanillaKeyReleased(key, scan, mods));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return runtime.charTyped(codePoint, modifiers,
                (character, mods) -> nativeCharTyped(character, mods) || vanillaCharTyped(character, mods));
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
