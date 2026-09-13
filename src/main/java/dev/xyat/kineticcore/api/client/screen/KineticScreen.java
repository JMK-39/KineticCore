package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public abstract class KineticScreen extends Screen {
    private enum CanvasMode {
        FIT,
        FLUID
    }

    public static final int STANDARD_CANVAS_WIDTH = 640;
    public static final int STANDARD_CANVAS_HEIGHT = 360;
    public static final int STANDARD_SAFE_MARGIN = 6;
    public static final int STANDARD_CONTROL_HEIGHT = 16;
    public static final int COMPACT_CONTROL_HEIGHT = 16;

    private int canvasWidth;
    private int canvasHeight;
    private float canvasScale;
    private int canvasX;
    private int canvasY;
    protected boolean renderRenderablesOnly;

    private float scaleMultiplier = 1f;
    private float minScale = 0.1f;
    private float maxScale = Float.MAX_VALUE;
    private float designWidth = STANDARD_CANVAS_WIDTH;
    private float designHeight = STANDARD_CANVAS_HEIGHT;
    private int safeMargin = STANDARD_SAFE_MARGIN;
    private CanvasMode canvasMode = CanvasMode.FIT;
    private GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(1, 1, 0);
    private GuiLayout.Metrics metrics = GuiLayout.measure(1, 1, 640, 360);
    private final GuiOverlay overlays = new GuiOverlay();
    private final Map<AbstractWidget, Component> widgetTooltips = new IdentityHashMap<>();
    private final List<ScrollViewportWidget> scrollViewportWidgets = new ArrayList<>();
    private GuiSession.DraftSession draftSession = new GuiSession.DraftSession();

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

    protected KineticScreen(Component title) {
        super(title);
    }

    /**
     * Uses the standard Kinetic editor canvas. The canvas always fits the available GUI area and is
     * allowed to scale above 1.0 on high-resolution displays.
     */
    protected final void useStandardCanvas() {
        useResponsiveCanvas(STANDARD_CANVAS_WIDTH, STANDARD_CANVAS_HEIGHT, STANDARD_SAFE_MARGIN);
    }

    /**
     * Uses a responsive fixed-aspect canvas. Scaling policy is owned by the API so child screens
     * cannot silently cap themselves to 1.0 and become undersized on 2K/4K displays.
     */
    protected final void useResponsiveCanvas(float designWidth, float designHeight, int safeMargin) {
        configureCanvas(designWidth, designHeight, safeMargin, CanvasMode.FIT, 1f, 0.1f, Float.MAX_VALUE);
    }

    /**
     * Compatibility alias. New editor screens should prefer {@link #useStandardCanvas()} or
     * {@link #useResponsiveCanvas(float, float, int)}.
     */
    protected final void useCanvas(float designWidth, float designHeight, int safeMargin) {
        useResponsiveCanvas(designWidth, designHeight, safeMargin);
    }

    /**
     * Uses a fixed-aspect canvas that will never scale above 1.0. This is reserved for interfaces
     * whose pixel size is part of their functional contract.
     */
    protected final void useFixedCanvas(float designWidth, float designHeight, int safeMargin) {
        configureCanvas(designWidth, designHeight, safeMargin, CanvasMode.FIT, 1f, 0.1f, 1f);
    }

    protected final void useFluidCanvas(float preferredWidth, float preferredHeight, int safeMargin) {
        configureCanvas(preferredWidth, preferredHeight, safeMargin, CanvasMode.FLUID, 1f, 0.1f, Float.MAX_VALUE);
    }

    private void configureCanvas(
            float designWidth,
            float designHeight,
            int safeMargin,
            CanvasMode mode,
            float scaleMultiplier,
            float minScale,
            float maxScale
    ) {
        this.designWidth = Math.max(1f, designWidth);
        this.designHeight = Math.max(1f, designHeight);
        this.safeMargin = Math.max(0, safeMargin);
        this.canvasMode = mode == null ? CanvasMode.FIT : mode;
        this.scaleMultiplier = Math.max(0.0001f, scaleMultiplier);
        this.minScale = Math.max(0.0001f, minScale);
        this.maxScale = Math.max(this.minScale, maxScale);
    }

    public final int canvasWidth() {
        return canvasWidth;
    }

    public final int canvasHeight() {
        return canvasHeight;
    }

    public final float canvasScale() {
        return canvasScale;
    }

    public final int canvasX() {
        return canvasX;
    }

    public final int canvasY() {
        return canvasY;
    }

    public final GuiLayout.SafeArea safeArea() {
        return safeArea;
    }

    public final GuiLayout.Metrics layout() {
        return metrics;
    }

    public final GuiLayout.Level layoutLevel() {
        return metrics.level();
    }

    public final boolean isPortraitLayout() {
        return metrics.isPortrait();
    }

    public final boolean isUltrawideLayout() {
        return metrics.isUltrawide();
    }

    public final boolean isCompactLayout() {
        return metrics.isCompact();
    }

    protected final GuiOverlay overlays() {
        return overlays;
    }

    public final EditBox addTextField(
            int x,
            int y,
            int width,
            Component message
    ) {
        return addTextField(x, y, width, message, null);
    }

    public final EditBox addTextField(
            int x,
            int y,
            int width,
            Component message,
            Component tooltip
    ) {
        EditBox box = createTextField(x, y, width, message, tooltip);
        addRenderableWidget(box);
        return box;
    }

    public final EditBox createTextField(
            int x,
            int y,
            int width,
            Component message,
            Component tooltip
    ) {
        EditBox box = new KineticWidgets.KineticEditBox(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.AutoCompleteBox addAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        KineticWidgets.AutoCompleteBox box = createAutoCompleteField(
                x, y, width, message, dictionarySupplier, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.AutoCompleteBox createAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        KineticWidgets.AutoCompleteBox box = new KineticWidgets.AutoCompleteBox(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = createIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox createIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.NumericAutoCompleteBox.integer(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                allowNegative, minValue, maxValue
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = createDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericAutoCompleteBox createDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericAutoCompleteBox box = KineticWidgets.NumericAutoCompleteBox.decimal(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                dictionarySupplier,
                allowNegative, minValue, maxValue
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = createIntegerField(
                x, y, width, message, allowNegative, minValue, maxValue, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericEditBox createIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.integer(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = createLongField(
                x, y, width, message, allowNegative, minValue, maxValue, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericEditBox createLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.longInteger(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final KineticWidgets.NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = createDecimalField(
                x, y, width, message, allowNegative, minValue, maxValue, tooltip
        );
        addRenderableWidget(box);
        return box;
    }

    public final KineticWidgets.NumericEditBox createDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        KineticWidgets.NumericEditBox box = KineticWidgets.NumericEditBox.decimal(
                font, x, y, width, STANDARD_CONTROL_HEIGHT,
                message == null ? Component.empty() : message,
                allowNegative, minValue, maxValue
        );
        registerWidgetTooltip(box, tooltip);
        return box;
    }

    public final void renderTextFieldPlaceholder(
            GuiGraphics graphics,
            EditBox box,
            Component placeholder
    ) {
        if (graphics == null || box == null || placeholder == null
                || !box.visible || !box.getValue().isEmpty() || box.isFocused()) {
            return;
        }
        String text = font.plainSubstrByWidth(
                placeholder.getString(),
                Math.max(0, box.getWidth() - 10)
        );
        graphics.drawString(
                font,
                text,
                box.getX() + 5,
                box.getY() + (box.getHeight() - font.lineHeight) / 2,
                GuiTheme.current().mutedText(),
                false
        );
    }

    public final Button addButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return addButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final Button addButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        return addButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final Button createButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return createButton(
                x, y, width, text, tooltip,
                action == null ? null : ignored -> action.run()
        );
    }

    public final Button createButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                action == null ? ignored -> { } : action,
                null,
                0
        );
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private Button addButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, height,
                text == null ? Component.empty() : text,
                ignored -> {
                    if (action != null) action.run();
                },
                null,
                0
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    private Button addButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, height,
                text == null ? Component.empty() : text,
                pressed -> {
                    if (action != null) action.onPress(pressed);
                },
                null,
                0
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Runnable action
    ) {
        return addButton(x, y, width, COMPACT_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final Button addCompactButton(
            int x, int y, int width, Component text, Component tooltip, Button.OnPress action
    ) {
        return addButton(x, y, width, COMPACT_CONTROL_HEIGHT, text, tooltip, action);
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        return addHighZButton(x, y, width, STANDARD_CONTROL_HEIGHT, text, tooltip, zLevel, action);
    }

    public final KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = createHighZButton(x, y, width, text, tooltip, zLevel, action);
        addRenderableWidget(button);
        return button;
    }

    public final KineticWidgets.HighZButton createHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, STANDARD_CONTROL_HEIGHT,
                text == null ? Component.empty() : text,
                pressed -> {
                    if (action != null) action.onPress(pressed);
                },
                null,
                zLevel
        );
        if (tooltip != null && !tooltip.getString().isBlank()) {
            registerWidgetTooltip(button, tooltip);
        }
        return button;
    }

    private KineticWidgets.HighZButton addHighZButton(
            int x,
            int y,
            int width,
            int height,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, height,
                text == null ? Component.empty() : text,
                ignored -> {
                    if (action != null) action.run();
                },
                null,
                zLevel
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return addToggleButton(x, y, width, STANDARD_CONTROL_HEIGHT, value, onText, offText, tooltip, responder);
    }

    private KineticWidgets.ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            int height,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        KineticWidgets.ToggleButton button = new KineticWidgets.ToggleButton(
                x, y, width, height, value,
                onText == null ? Component.empty() : onText,
                offText == null ? Component.empty() : offText,
                responder
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final Button addCompactScrollableButton(
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

    public final Button addScrollableButton(
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

    private Button addScrollableButton(
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
        KineticWidgets.HighZButton button = new KineticWidgets.HighZButton(
                x, y, width, height,
                text == null ? Component.empty() : text,
                ignored -> {
                    if (action != null) action.run();
                },
                null,
                0
        );
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

    public final KineticWidgets.ColorSwatchButton addColorSwatchButton(
            int x, int y, int rgb, Component tooltip, Runnable action
    ) {
        KineticWidgets.ColorSwatchButton button = new KineticWidgets.ColorSwatchButton(
                x, y, COMPACT_CONTROL_HEIGHT, rgb,
                ignored -> { if (action != null) action.run(); }
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final KineticWidgets.ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        KineticWidgets.ColorPreviewButton button = new KineticWidgets.ColorPreviewButton(
                x, y, width, STANDARD_CONTROL_HEIGHT, color,
                text == null ? Component.empty() : text,
                ignored -> {
                    if (action != null) action.run();
                }
        );
        addRenderableWidget(button);
        registerWidgetTooltip(button, tooltip);
        return button;
    }

    public final void closeContextMenu() {
        overlays.closeMenu();
    }

    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        if (widget == null) return null;
        if (tooltip == null || tooltip.getString().isBlank()) widgetTooltips.remove(widget);
        else widgetTooltips.put(widget, tooltip);
        return widget;
    }

    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        if (widget == null) return null;
        addRenderableWidget(widget);
        if (tooltip != null && !tooltip.getString().isBlank()) {
            registerWidgetTooltip(widget, tooltip);
        }
        return widget;
    }

    public final void removeControl(AbstractWidget widget) {
        if (widget == null) return;
        widgetTooltips.remove(widget);
        scrollViewportWidgets.removeIf(entry -> entry.widget() == widget);
        removeWidget(widget);
    }

    private void requestWidgetTooltip(double mouseX, double mouseY) {
        for (Map.Entry<AbstractWidget, Component> entry : widgetTooltips.entrySet()) {
            AbstractWidget widget = entry.getKey();
            if (widget == null || !widget.visible) continue;
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight()) {
                overlays.tooltip(entry.getValue(), 320);
                return;
            }
        }
    }

    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        addWidget(list);
        return list;
    }

    public final void resetScrollableWidgets() {
        scrollViewportWidgets.clear();
    }

    public final <T extends AbstractWidget> T addScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        addRenderableWidget(widget);
        return bindScrollableWidget(widget, left, top, right, bottom, pixelOffset);
    }

    /**
     * Registers a widget that was already created through a KineticScreen API factory as belonging
     * to a scrolling viewport. Child screens must use this instead of re-adding the widget.
     */
    public final <T extends AbstractWidget> T bindScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (widget == null) return null;
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

    /**
     * Makes an arbitrary widget participate in a Kinetic scrolling viewport. Widgets created by an
     * API factory are only bound; custom widgets that are not yet attached to the screen are added
     * once before being bound.
     */
    public final <T extends AbstractWidget> T attachScrollableWidget(
            T widget,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (widget == null) return null;
        if (!children().contains(widget)) addRenderableWidget(widget);
        return bindScrollableWidget(widget, left, top, right, bottom, pixelOffset);
    }

    private void updateScrollableWidgetPositions() {
        Map<ScrollViewportKey, Double> offsets = new HashMap<>();
        for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
            double offset = offsets.computeIfAbsent(
                    viewportWidget.key(),
                    ignored -> Math.max(0D, viewportWidget.pixelOffset().getAsDouble())
            );
            viewportWidget.widget().setY(viewportWidget.baseY() - (int) Math.round(offset));
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
            int widgetBottom = widgetTop + widget.getHeight();
            if (widgetBottom <= viewportWidget.top() || widgetTop >= viewportWidget.bottom()) continue;

            enableCanvasScissor(
                    graphics,
                    viewportWidget.left(),
                    viewportWidget.top(),
                    viewportWidget.right(),
                    viewportWidget.bottom()
            );
            try {
                widget.render(graphics, mouseX, mouseY, partialTick);
            } finally {
                disableCanvasScissor(graphics);
            }
        }
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


    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, true);
    }







    protected final void commitDraft() {
        draftSession.commitBaseline(this);
    }

    protected final void discardDraft() {
        draftSession.discardToBaseline();
    }

    protected final boolean hasUnsavedEdits() {
        return draftSession.isDirty();
    }


    public final void showTooltip(Component component) {
        overlays.tooltip(component);
    }

    public final void showTooltip(List<? extends Component> lines) {
        overlays.tooltip(lines);
    }

    public final void showTooltip(Component component, int maxWidth) {
        overlays.tooltip(component, maxWidth);
    }

    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        overlays.tooltip(lines, maxWidth);
    }

    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        overlays.formattedTooltip(lines);
    }

    public final void showItemTooltip(ItemStack stack) {
        overlays.itemTooltip(stack);
    }

    public final void openContextMenu(double virtualX, double virtualY, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu(toScreenX(virtualX), toScreenY(virtualY), items);
    }

    public final void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        overlays.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return addDropdown(x, y, width, options, List.of(), selectedIndex, tooltip, responder);
    }

    public final KineticWidgets.Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        List<Component> normalizedOptions = options == null ? new ArrayList<>() : new ArrayList<>(options);
        List<Component> normalizedTooltips = optionTooltips == null ? new ArrayList<>() : new ArrayList<>(optionTooltips);
        KineticWidgets.Dropdown control = dropdown(
                x, y, width, STANDARD_CONTROL_HEIGHT, normalizedOptions, normalizedTooltips, selectedIndex, responder
        );
        addRenderableWidget(control);
        registerWidgetTooltip(control, tooltip);
        return control;
    }

    private KineticWidgets.Dropdown dropdown(
            int x,
            int y,
            int width,
            int height,
            List<Component> options,
            List<Component> optionTooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return new KineticWidgets.Dropdown(
                x, y, width, height, options, selectedIndex, responder, control -> {
                    List<GuiOverlay.MenuItem> entries = new ArrayList<>();
                    List<Component> values = control.options();
                    for (int index = 0; index < values.size(); index++) {
                        int optionIndex = index;
                        Component optionTooltip = index < optionTooltips.size() && optionTooltips.get(index) != null
                                && !optionTooltips.get(index).getString().isBlank()
                                ? optionTooltips.get(index)
                                : values.get(index);
                        entries.add(GuiOverlay.MenuItem.toggle(
                                values.get(index),
                                optionTooltip,
                                optionIndex == control.selectedIndex(),
                                () -> control.choose(optionIndex)
                        ));
                    }
                    openContextMenu(x, y + height, entries);
                }
        );
    }

    @Override
    protected final void init() {
        super.init();
        updateMetrics();
        rebuildUi();
    }

    public final void rebuildUi() {
        clearWidgets();
        widgetTooltips.clear();
        scrollViewportWidgets.clear();
        buildUi();
    }

    protected abstract void buildUi();

    private void updateMetrics() {
        safeArea = GuiLayout.SafeArea.of(width, height, safeMargin);
        metrics = GuiLayout.measure(safeArea.width(), safeArea.height(), designWidth, designHeight);
        if (canvasMode == CanvasMode.FLUID) updateFluidMetrics();
        else updateFixedMetrics();
    }

    private void updateFixedMetrics() {
        float fitScale = Math.max(0.0001f, metrics.fitScale());
        float requested = fitScale * Math.max(0.0001f, scaleMultiplier);
        float lower = Math.max(0.0001f, minScale);
        float upper = Math.max(lower, maxScale);
        canvasScale = Math.min(fitScale, Math.max(lower, Math.min(requested, upper)));
        canvasWidth = Math.max(1, Math.round(designWidth));
        canvasHeight = Math.max(1, Math.round(designHeight));
        canvasX = safeArea.left() + Math.round((safeArea.width() - designWidth * canvasScale) / 2f);
        canvasY = safeArea.top() + Math.round((safeArea.height() - designHeight * canvasScale) / 2f);
    }

    private void updateFluidMetrics() {
        float lower = Math.max(0.0001f, minScale);
        float upper = Math.max(lower, maxScale);
        canvasScale = Math.max(lower, Math.min(Math.max(0.0001f, scaleMultiplier), upper));
        canvasX = safeArea.left();
        canvasY = safeArea.top();
        canvasWidth = Math.max(1, (int) Math.floor(safeArea.width() / canvasScale));
        canvasHeight = Math.max(1, (int) Math.floor(safeArea.height() / canvasScale));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        overlays.beginFrame();
        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));

        GuiGraphics canvasGraphics = new CanvasGuiGraphics(graphics);
        canvasGraphics.pose().pushPose();
        canvasGraphics.pose().translate(canvasX, canvasY, 0);
        canvasGraphics.pose().scale(canvasScale, canvasScale, 1f);
        try {
            updateScrollableWidgetPositions();
            updateCanvasWidgets(virtualMouseX, virtualMouseY, partialTick);
            renderCanvasBackground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);

            List<AbstractWidget> temporarilyHidden = new ArrayList<>();
            for (ScrollViewportWidget viewportWidget : scrollViewportWidgets) {
                AbstractWidget widget = viewportWidget.widget();
                if (widget.visible) {
                    widget.visible = false;
                    temporarilyHidden.add(widget);
                }
            }
            try {
                if (renderRenderablesOnly) {
                    for (Renderable renderable : renderables) {
                        renderable.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
                    }
                } else {
                    super.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
                }
            } finally {
                for (AbstractWidget widget : temporarilyHidden) {
                    widget.visible = true;
                }
            }

            renderScrollableWidgets(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
            renderCanvasForeground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
        } finally {
            canvasGraphics.pose().popPose();
        }

        if (!overlays.blocksInput()) {
            requestWidgetTooltip(virtualMouseX, virtualMouseY);
            renderTooltips(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
        }
        renderScreenOverlay(graphics, virtualMouseX, virtualMouseY, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, width, height, mouseX, mouseY);
    }

    private final class CanvasGuiGraphics extends GuiGraphics {
        private CanvasGuiGraphics(GuiGraphics source) {
            super(KineticScreen.this.minecraft, source.bufferSource());
        }

        @Override
        public void enableScissor(int left, int top, int right, int bottom) {
            super.enableScissor(
                    toScreenX(left),
                    toScreenY(top),
                    toScreenRight(right),
                    toScreenBottom(bottom)
            );
        }
    }

    protected void updateCanvasWidgets(int mouseX, int mouseY, float partialTick) {
    }

    protected void renderCanvasBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderCanvasForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderTooltips(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY
    ) {
    }

    protected void renderScreenOverlay(
            GuiGraphics graphics,
            int virtualMouseX,
            int virtualMouseY,
            int screenMouseX,
            int screenMouseY,
            float partialTick
    ) {
    }

    public final double toVirtualX(double screenX) {
        return (screenX - canvasX) / canvasScale;
    }

    public final double toVirtualY(double screenY) {
        return (screenY - canvasY) / canvasScale;
    }

    public final int toScreenX(double virtualX) {
        return canvasX + (int) Math.floor(virtualX * canvasScale);
    }

    public final int toScreenY(double virtualY) {
        return canvasY + (int) Math.floor(virtualY * canvasScale);
    }

    public final int toScreenRight(double virtualX) {
        return canvasX + (int) Math.ceil(virtualX * canvasScale);
    }

    public final int toScreenBottom(double virtualY) {
        return canvasY + (int) Math.ceil(virtualY * canvasScale);
    }

    public final boolean isInsideCanvas(double screenX, double screenY) {
        double virtualX = toVirtualX(screenX);
        double virtualY = toVirtualY(screenY);
        return virtualX >= 0 && virtualX < canvasWidth && virtualY >= 0 && virtualY < canvasHeight;
    }

    public final void enableCanvasScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof CanvasGuiGraphics) {
            graphics.enableScissor(left, top, right, bottom);
            return;
        }
        graphics.enableScissor(toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    public final void disableCanvasScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    public final void renderScaledList(
            ObjectSelectionList<?> list,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (list == null || minecraft == null) return;
        if (graphics instanceof CanvasGuiGraphics) {
            list.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        GuiGraphics proxy = new CanvasGuiGraphics(graphics);
        proxy.pose().pushPose();
        proxy.pose().translate(canvasX, canvasY, 0);
        proxy.pose().scale(canvasScale, canvasScale, 1f);
        try {
            list.render(proxy, mouseX, mouseY, partialTick);
        } finally {
            proxy.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        boolean handled = canvasMouseClicked(toVirtualX(mouseX), toVirtualY(mouseY), button);
        if (!handled) {
            setFocused(null);
        }
        return handled;
    }

    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        updateScrollableWidgetPositions();
        List<AbstractWidget> hidden = hideScrollableWidgetsOutsideViewport(mouseX, mouseY);
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            restoreScrollableWidgetVisibility(hidden);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        return canvasMouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
    }

    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        return canvasMouseDragged(
                toVirtualX(mouseX),
                toVirtualY(mouseY),
                button,
                dragX / canvasScale,
                dragY / canvasScale
        );
    }

    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        return canvasMouseScrolled(toVirtualX(mouseX), toVirtualY(mouseY), delta);
    }

    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (GuiSession.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!overlays.blocksInput()) canvasMouseMoved(toVirtualX(mouseX), toVirtualY(mouseY));
    }

    protected void canvasMouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (overlays.keyPressed(keyCode)) return true;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    final GuiSession.DraftSession draftSession() {
        return draftSession;
    }

    final void adoptDraftSession(GuiSession.DraftSession sharedDraft) {
        if (sharedDraft != null && sharedDraft.enabled()) {
            this.draftSession = sharedDraft;
        }
    }

    final void discardPendingEditsForNavigation() {
        draftSession.discardToBaseline();
    }

    public final void navigateBack() {
        GuiSession.back(this);
    }

    @Override
    public void onClose() {
        navigateBack();
    }
}
