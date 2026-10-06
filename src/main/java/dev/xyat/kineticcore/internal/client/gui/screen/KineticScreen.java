package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import javax.annotation.Nonnull;

import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.gui.layout.KineticLayout;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * Base screen for Kinetic interfaces using the fixed 640x360 virtual canvas.
 * <p>
 * The shared control, overlay, focus and draft API comes from {@link KineticScreenHost}; this class only adds the
 * responsive canvas and controls bound to scrolling viewports inside it.
 */
public abstract class KineticScreen extends Screen implements KineticScreenHost {
    /** Standard logical canvas width used by responsive Kinetic screens. */
    public static final int STANDARD_CANVAS_WIDTH = KineticLayout.MAX_CANVAS_WIDTH;
    /** Standard logical canvas height used by responsive Kinetic screens. */
    public static final int STANDARD_CANVAS_HEIGHT = KineticLayout.MAX_CANVAS_HEIGHT;
    /** Standard safe-area margin applied before logical canvas measurement. */
    public static final int STANDARD_SAFE_MARGIN = 6;
    /** Standard API-defined control height. */
    public static final int STANDARD_CONTROL_HEIGHT = 16;
    /** Compact API-defined control height. */
    public static final int COMPACT_CONTROL_HEIGHT = 16;
    /** API-defined height for item-backed card buttons. */
    public static final int ITEM_BUTTON_HEIGHT = 38;
    /** Standard height for content-rich card buttons such as item, task, or skill cards. */
    public static final int CARD_BUTTON_HEIGHT = 26;

    private int requestedCanvasWidth = STANDARD_CANVAS_WIDTH;
    private int requestedCanvasHeight = STANDARD_CANVAS_HEIGHT;
    private int requestedSafeMargin = STANDARD_SAFE_MARGIN;
    private final List<ScrollViewportWidget> scrollViewportWidgets = new ArrayList<>();
    private final KineticScreenRuntime runtime = new KineticScreenRuntime(
            this,
            KineticCanvasTransform.canvas(STANDARD_CANVAS_WIDTH, STANDARD_CANVAS_HEIGHT),
            () -> font,
            () -> minecraft,
            this::addRenderableWidget,
            this::addWidget,
            widget -> {
                scrollViewportWidgets.removeIf(entry -> entry.widget() == widget);
                removeWidget(widget);
            }
    );

    private record ScrollViewportKey(int left, int top, int right, int bottom) {
    }

    private record ScrollViewportWidget(
            AbstractWidget widget,
            int baseY,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        ScrollViewportKey key() {
            return new ScrollViewportKey(left, top, right, bottom);
        }
    }

    /** Creates a new {@code KineticScreen}. */
    protected KineticScreen(Component title) {
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
        return canvasWidth();
    }

    @Override
    public final int pageHeight() {
        return canvasHeight();
    }

    // ---- canvas ------------------------------------------------------------------------------------------------

    /**
     * Selects the logical canvas used by this screen while keeping scaling, safe-area handling,
     * coordinate conversion, clipping, and high-DPI behavior owned by KineticCore.
     */
    protected final void useCanvas(float designWidth, float designHeight, int safeMargin) {
        requestedCanvasWidth = Math.max(1, Math.min(KineticLayout.MAX_CANVAS_WIDTH,
                Math.round(Float.isFinite(designWidth) ? designWidth : STANDARD_CANVAS_WIDTH)));
        requestedCanvasHeight = Math.max(1, Math.min(KineticLayout.MAX_CANVAS_HEIGHT,
                Math.round(Float.isFinite(designHeight) ? designHeight : STANDARD_CANVAS_HEIGHT)));
        requestedSafeMargin = Math.max(0, safeMargin);
    }

    /** Returns the fixed virtual canvas width. */
    public final int canvasWidth() {
        return runtime.canvas().width();
    }

    /** Returns the fixed virtual canvas height. */
    public final int canvasHeight() {
        return runtime.canvas().height();
    }

    /** Returns the scale from virtual canvas coordinates to screen coordinates. */
    public final float canvasScale() {
        return runtime.canvas().scale();
    }

    /** Returns the screen-space X origin of the virtual canvas. */
    public final int canvasX() {
        return runtime.canvas().x();
    }

    /** Returns the screen-space Y origin of the virtual canvas. */
    public final int canvasY() {
        return runtime.canvas().y();
    }

    /** Returns safe area. */
    public final KineticLayout.SafeArea safeArea() {
        return runtime.canvas().safeArea();
    }

    /** Returns layout. */
    public final KineticLayout.Metrics layout() {
        return runtime.canvas().metrics();
    }

    /** Returns layout level. */
    public final KineticLayout.Level layoutLevel() {
        return layout().level();
    }

    /** Returns whether the current virtual canvas uses portrait layout rules. */
    public final boolean isPortraitLayout() {
        return layout().isPortrait();
    }

    /** Returns whether the current virtual canvas uses ultrawide layout rules. */
    public final boolean isUltrawideLayout() {
        return layout().isUltrawide();
    }

    /** Returns whether the current virtual canvas uses compact spacing rules. */
    public final boolean isCompactLayout() {
        return layout().isCompact();
    }

    /** Returns whether the supplied screen-space point lies inside the Kinetic canvas. */
    public final boolean isInsideCanvas(double screenX, double screenY) {
        return runtime.canvas().contains(screenX, screenY);
    }

    /** Enables a scissor rectangle expressed in this screen's logical canvas coordinates. */
    protected final void enableCanvasScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics == null) return;
        graphics.enableScissor(left, top, right, bottom);
    }

    /** Disables a scissor rectangle previously enabled with {@link #enableCanvasScissor}. */
    protected final void disableCanvasScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        graphics.disableScissor();
    }

    /**
     * Registers a preview's logical-canvas hit area for this render frame.
     * Ctrl + wheel is dispatched here before autocomplete or grid scrolling.
     * The screen owns coordinate conversion; addons never move the physical cursor.
     */
    public final void registerPreviewWheelTarget(EntityPreviewRenderer renderer, String key,
                                                  int x, int y, int width, int height) {
        runtime.registerPreviewWheelTarget(renderer, key, x, y, width, height);
    }

    // ---- controls bound to a scrolling viewport ----------------------------------------------------------------

    /** Creates and registers a compact toggle bound to the standard Kinetic scrolling viewport. */
    public final ToggleButton addCompactScrollableToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableToggleButton(
                x, y, width, COMPACT_CONTROL_HEIGHT, value, onText, offText, tooltip, validator, responder,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    /** Creates and registers a standard-height toggle bound to the standard Kinetic scrolling viewport. */
    public final ToggleButton addScrollableToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableToggleButton(
                x, y, width, STANDARD_CONTROL_HEIGHT, value, onText, offText, tooltip, validator, responder,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    private ToggleButton addScrollableToggleButton(
            int x,
            int y,
            int width,
            int height,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        ToggleButton button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactToggleButton(x, y, width, value, onText, offText, null, validator, responder)
                : KineticWidgets.createToggleButton(x, y, width, value, onText, offText, null, validator, responder);
        addScrollableWidget(
                button,
                viewportLeft,
                viewportTop,
                viewportRight,
                viewportBottom,
                pixelOffset
        );
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final StateButton addCompactScrollableButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableButton(
                x, y, width, COMPACT_CONTROL_HEIGHT,
                text, tooltip, action,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final StateButton addScrollableButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        return addScrollableButton(
                x, y, width, STANDARD_CONTROL_HEIGHT,
                text, tooltip, action,
                viewportLeft, viewportTop, viewportRight, viewportBottom, pixelOffset
        );
    }

    private StateButton addScrollableButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Runnable action,
            int viewportLeft,
            int viewportTop,
            int viewportRight,
            int viewportBottom,
            DoubleSupplier pixelOffset
    ) {
        HighZButton button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactHighZButton(x, y, width, text, null, 0, action)
                : KineticWidgets.createHighZButton(x, y, width, text, null, 0, action);
        addScrollableWidget(
                button,
                viewportLeft,
                viewportTop,
                viewportRight,
                viewportBottom,
                pixelOffset
        );
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    /** Resets scrollable widgets. */
    public final void resetScrollableWidgets() {
        scrollViewportWidgets.clear();
    }

    /**
     * Adds or reuses one widget in a Kinetic scrolling viewport. Already registered widgets are not
     * registered a second time, and rebinding the same widget replaces its previous viewport binding.
     */
    public final <T extends InternalControl> T addScrollableWidget(
            T control,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (control == null) return null;
        AbstractWidget widget = KineticControlBridge.widget(control);
        if (!children().contains(widget)) runtime.controls().registerWidget(widget, null);
        bindScrollableWidgetInternal(widget, left, top, right, bottom, pixelOffset);
        return control;
    }

    private <T extends AbstractWidget> T bindScrollableWidgetInternal(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        java.util.Objects.requireNonNull(pixelOffset, "pixelOffset");
        scrollViewportWidgets.removeIf(viewportWidget -> viewportWidget.widget() == widget);
        scrollViewportWidgets.add(new ScrollViewportWidget(
                widget,
                widget.getY(),
                left,
                top,
                right,
                bottom,
                pixelOffset
        ));
        return widget;
    }

    private boolean isWidgetTooltipHitAllowed(AbstractWidget widget, double mouseX, double mouseY) {
        boolean boundToViewport = false;
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            if (viewportWidget.widget() != widget) continue;
            boundToViewport = true;
            if (mouseX >= viewportWidget.left()
                    && mouseX < viewportWidget.right()
                    && mouseY >= viewportWidget.top()
                    && mouseY < viewportWidget.bottom()) {
                return true;
            }
        }
        return !boundToViewport;
    }

    private void updateScrollableWidgetPositions() {
        Map<ScrollViewportKey, Double> offsets = new HashMap<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            double offset = offsets.computeIfAbsent(
                    viewportWidget.key(),
                    ignored -> {
                        double requested = viewportWidget.pixelOffset().getAsDouble();
                        return Double.isFinite(requested) ? Math.max(0D, requested) : 0D;
                    }
            );
            AbstractWidget widget = viewportWidget.widget();
            // Round before subtraction and clamp the screen coordinate. Casting a very
            // large scroll offset to int would wrap and move an off-screen control forward.
            double targetY = (double) viewportWidget.baseY() - Math.round(offset);
            widget.setY((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, targetY)));
            int widgetTop = widget.getY();
            long widgetBottom = (long) widgetTop + widget.getHeight();
            if (runtime.focus().isControlFocused(widget)
                    && (widgetBottom <= viewportWidget.top() || widgetTop >= viewportWidget.bottom())) {
                runtime.focus().blurControl(widget);
            }
        }
    }

    private void renderScrollableWidgets(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            AbstractWidget widget = viewportWidget.widget();
            if (!widget.visible) continue;
            int widgetTop = widget.getY();
            long widgetBottom = (long) widgetTop + widget.getHeight();
            if (widgetBottom <= viewportWidget.top() || widgetTop >= viewportWidget.bottom()) continue;

            enableUiScissor(
                    graphics,
                    viewportWidget.left(),
                    viewportWidget.top(),
                    viewportWidget.right(),
                    viewportWidget.bottom()
            );
            try {
                VanillaGuiDraw.render(widget, graphics, mouseX, mouseY, partialTick);
            } finally {
                disableUiScissor(graphics);
            }
        }
    }

    private List<AbstractWidget> hideScrollableWidgetsForDefaultRender() {
        List<AbstractWidget> hidden = new ArrayList<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            AbstractWidget widget = viewportWidget.widget();
            if (!widget.visible) continue;
            widget.visible = false;
            hidden.add(widget);
        }
        return hidden;
    }

    private List<AbstractWidget> hideScrollableWidgetsOutsideViewport(double mouseX, double mouseY) {
        List<AbstractWidget> hidden = new ArrayList<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            AbstractWidget widget = viewportWidget.widget();
            if (!widget.visible) continue;
            boolean insideViewport = mouseX >= viewportWidget.left()
                    && mouseX < viewportWidget.right()
                    && mouseY >= viewportWidget.top()
                    && mouseY < viewportWidget.bottom();
            if (!insideViewport) {
                widget.visible = false;
                hidden.add(widget);
            }
        }
        return hidden;
    }

    private void restoreScrollableWidgetVisibility(List<AbstractWidget> widgets) {
        for (AbstractWidget widget : widgets) {
            widget.visible = true;
        }
    }

    // ---- lifecycle ---------------------------------------------------------------------------------------------

    @Override
    protected final void init() {
        super.init();
        runtime.canvas().fit(width, height, requestedCanvasWidth, requestedCanvasHeight, requestedSafeMargin);
        rebuildUi();
    }

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    @Override
    public final void rebuildUi() {
        clearControlFocus();
        closeContextMenu();
        clearWidgets();
        runtime.controls().clear();
        scrollViewportWidgets.clear();
        buildUi();
    }

    /** Builds this screen's API-managed controls for the current UI rebuild. */
    protected abstract void buildUi();

    /** {@inheritDoc} */
    @Override
    public final void tick() {
        super.tick();
        runtime.tick(this::canvasTick);
    }

    /** Handles business per-tick work after standard registered controls have ticked. */
    protected void canvasTick() {
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

    /** Renders content behind registered controls in virtual-canvas coordinates. */
    protected void renderCanvasBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** Renders content above registered controls in virtual-canvas coordinates. */
    protected void renderCanvasForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    /** Requests business tooltips after registered control tooltips have been checked. */
    protected void renderTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        runtime.render(graphics, mouseX, mouseY, partialTick,
                (frame, frameMouseX, frameMouseY, frameTick) -> runtime.renderInCanvas(
                        frame, frameMouseX, frameMouseY, frameTick,
                        (canvas, virtualMouseX, virtualMouseY, canvasTick) ->
                                renderCanvasFrame(canvas, virtualMouseX, virtualMouseY, mouseX, mouseY, canvasTick)));
    }

    //? if >=1.20.2 <26.1 {
    /*// Screen.render draws the background itself since 1.20.2; KineticScreenRuntime already drew it this frame.
    @Override
    public void renderBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }
    *///?}
    //? if >=26.1 {
    /*// Screen.extractRenderState draws the background itself; KineticScreenRuntime already drew it this frame.
    @Override
    public void extractBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    private void renderCanvasFrame(GuiGraphics canvasGraphics, int virtualMouseX, int virtualMouseY,
                                   int screenMouseX, int screenMouseY, float partialTick) {
        updateScrollableWidgetPositions();
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.beginFrame(canvasGraphics);
        renderCanvasBackground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);

        List<AbstractWidget> hiddenScrollableWidgets = hideScrollableWidgetsForDefaultRender();
        try {
            vanillaRender(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            restoreScrollableWidgetVisibility(hiddenScrollableWidgets);
        }

        renderScrollableWidgets(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        renderCanvasForeground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        if (dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.ENABLED) {
            List<AbstractWidget> shown = new java.util.ArrayList<>();
            for (var child : children()) {
                if (child instanceof AbstractWidget widget && !hiddenScrollableWidgets.contains(widget)) shown.add(widget);
            }
            dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.controls(canvasGraphics, shown);
        }
        dev.xyat.kineticcore.internal.client.gui.LayoutFrameRecorder.endFrame();
        runtime.controls().renderAutoCompleteSuggestions(canvasGraphics, virtualMouseX, virtualMouseY);

        if (runtime.businessTooltipsAllowed(isInsideCanvas(screenMouseX, screenMouseY))
                && !runtime.requestWidgetTooltip(virtualMouseX, virtualMouseY,
                widget -> isWidgetTooltipHitAllowed(widget, virtualMouseX, virtualMouseY))) {
            renderTooltips(canvasGraphics, virtualMouseX, virtualMouseY, screenMouseX, screenMouseY);
        }
    }

    // ---- input -------------------------------------------------------------------------------------------------

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return runtime.mouseClicked(mouseX, mouseY, button, this::canvasMouseClicked);
    }

    /** Handles a mouse click after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        updateScrollableWidgetPositions();
        List<AbstractWidget> hidden = hideScrollableWidgetsOutsideViewport(mouseX, mouseY);
        try {
            return vanillaMouseClicked(mouseX, mouseY, button);
        } finally {
            restoreScrollableWidgetVisibility(hidden);
        }
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return runtime.mouseReleased(mouseX, mouseY, button, this::canvasMouseReleased);
    }

    /** Handles mouse release after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return vanillaMouseReleased(mouseX, mouseY, button);
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return runtime.mouseDragged(mouseX, mouseY, button, dragX, dragY, this::canvasMouseDragged);
    }

    /** Handles mouse dragging after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return vanillaMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** {@inheritDoc} */
    //? if >=1.20.2 {
    /*@Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
    *///?} else {
    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    //?}
        return runtime.mouseScrolled(mouseX, mouseY, delta, this::canvasMouseScrolled);
    }

    /** Handles mouse scrolling in virtual canvas coordinates. */
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (GuiSessionRuntime.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        //? if >=1.20.2 {
        /*return super.mouseScrolled(mouseX, mouseY, 0.0D, delta);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, delta);
        //?}
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        runtime.mouseMoved(mouseX, mouseY, (virtualMouseX, virtualMouseY) -> {
            canvasMouseMoved(virtualMouseX, virtualMouseY);
            super.mouseMoved(virtualMouseX, virtualMouseY);
        });
    }

    /** Handles business mouse movement in virtual canvas coordinates after Overlay routing. */
    protected void canvasMouseMoved(double mouseX, double mouseY) {
    }

    /** Handles business key presses after Overlay and autocomplete routing, before default Escape navigation. */
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business key releases after Overlay routing, before registered controls. */
    protected boolean canvasKeyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /** Handles business character input after Overlay routing, before registered controls. */
    protected boolean canvasCharTyped(char codePoint, int modifiers) {
        return false;
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return runtime.keyPressed(keyCode, scanCode, modifiers, this::canvasKeyPressed, this::vanillaKeyPressed);
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return runtime.keyReleased(keyCode, scanCode, modifiers,
                (key, scan, mods) -> canvasKeyReleased(key, scan, mods) || vanillaKeyReleased(key, scan, mods));
    }

    /** {@inheritDoc} */
    //? if <26.1
    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        return runtime.charTyped(codePoint, modifiers,
                (character, mods) -> canvasCharTyped(character, mods) || vanillaCharTyped(character, mods));
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
