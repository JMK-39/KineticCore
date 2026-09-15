package dev.xyat.kineticcore.api.client.screen;

import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class KineticScreen extends Screen {
    public static final int STANDARD_CANVAS_WIDTH = 640;
    public static final int STANDARD_CANVAS_HEIGHT = 360;
    public static final int STANDARD_SAFE_MARGIN = 6;
    public static final int STANDARD_CONTROL_HEIGHT = 16;
    public static final int COMPACT_CONTROL_HEIGHT = 16;

    private int canvasWidth = STANDARD_CANVAS_WIDTH;
    private int canvasHeight = STANDARD_CANVAS_HEIGHT;
    private float canvasScale;
    private int canvasX;
    private int canvasY;
    protected boolean renderRenderablesOnly;
    private GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(1, 1, 0);
    private GuiLayout.Metrics metrics = GuiLayout.measure(1, 1, 640, 360);
    private final GuiOverlay overlays = new GuiOverlay();
    private final List<ScrollViewportWidget> scrollViewportWidgets = new ArrayList<>();
    private final KineticScreenControls controls = new KineticScreenControls(
            () -> font, overlays, this::addRenderableWidget, this::addWidget,
            widget -> { scrollViewportWidgets.removeIf(entry -> entry.widget() == widget); removeWidget(widget); }, this::openContextMenu
    );
    private final KineticScreenFocus focus = new KineticScreenFocus(this);
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
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.ensureReady();
    }

    public final void useStandardCanvas() {
        configureCanvas(STANDARD_CANVAS_WIDTH, STANDARD_CANVAS_HEIGHT, STANDARD_SAFE_MARGIN);
    }

    public final void useResponsiveCanvas(float designWidth, float designHeight, int safeMargin) {
        configureCanvas(designWidth, designHeight, safeMargin);
    }

    public final void useFluidCanvas(float designWidth, float designHeight, int safeMargin) {
        configureCanvas(designWidth, designHeight, safeMargin);
    }

    public final void useResponsiveContainer(float designWidth, float designHeight, int safeMargin) {
        configureCanvas(designWidth, designHeight, safeMargin);
    }

    private void configureCanvas(float designWidth, float designHeight, int safeMargin) {
        int resolvedWidth = Math.max(1, Math.round(designWidth));
        int resolvedHeight = Math.max(1, Math.round(designHeight));
        safeArea = GuiLayout.SafeArea.of(width, height, Math.max(0, safeMargin));
        metrics = GuiLayout.measure(safeArea.width(), safeArea.height(), resolvedWidth, resolvedHeight);
        canvasScale = Math.max(0.0001f, metrics.fitScale());
        canvasWidth = resolvedWidth;
        canvasHeight = resolvedHeight;
        canvasX = safeArea.left() + Math.round((safeArea.width() - resolvedWidth * canvasScale) / 2f);
        canvasY = safeArea.top() + Math.round((safeArea.height() - resolvedHeight * canvasScale) / 2f);
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

    /** 创建未注册的标准输入框；用于需要先配置后再通过 addControl 注册的 Screen 逻辑。 */
    public final KineticEditBox createTextField(int x, int y, int width, Component message, Component tooltip) {
        return KineticWidgets.createTextField(font, x, y, width, message, tooltip);
    }

    /** 创建未注册的标准整数输入框；数值规则完全由调用方传入。 */
    public final NumericEditBox createIntegerField(
            int x, int y, int width, Component message,
            boolean allowNegative, Integer minValue, Integer maxValue, Component tooltip
    ) {
        return KineticWidgets.createIntegerField(font, x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的标准小数输入框；数值规则完全由调用方传入。 */
    public final NumericEditBox createDecimalField(
            int x, int y, int width, Component message,
            boolean allowNegative, Double minValue, Double maxValue, Component tooltip
    ) {
        return KineticWidgets.createDecimalField(font, x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建未注册的高层按钮；用于调用方随后通过 addControl 统一注册。 */
    public final HighZButton createHighZButton(
            int x, int y, int width, Component text, Component tooltip, int zLevel, Button.OnPress action
    ) {
        return KineticWidgets.createHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField( int x, int y, int width, Component message ) {
        return controls.addTextField(x, y, width, message);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField( int x, int y, int width, Component message, Component tooltip ) {
        return controls.addTextField(x, y, width, message, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField( int x, int y, int width, Component message, Component placeholder, Component tooltip ) {
        return controls.addTextField(x, y, width, message, placeholder, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final KineticEditBox addTextField(
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Predicate<String> validator,
            Component tooltip
    ) {
        return controls.addTextField(x, y, width, message, placeholder, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final MultiLineEditBox addMultiLineTextField(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Component placeholder,
            Component tooltip
    ) {
        return controls.addMultiLineTextField(x, y, width, height, message, placeholder, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final AutoCompleteBox addAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return controls.addAutoCompleteField(x, y, width, message, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final AutoCompleteBox addAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Component placeholder,
            Supplier<List<String>> dictionarySupplier,
            Component tooltip
    ) {
        return controls.addAutoCompleteField(x, y, width, message, placeholder, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
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
        return controls.addIntegerAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addIntegerAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addIntegerAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return controls.addLongAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addLongAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addLongAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
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
        return controls.addDecimalAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericAutoCompleteBox addDecimalAutoCompleteField(
            int x,
            int y,
            int width,
            Component message,
            Supplier<List<String>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addDecimalAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Component tooltip
    ) {
        return controls.addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Component tooltip
    ) {
        return controls.addLongField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addLongField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Component tooltip
    ) {
        return controls.addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return controls.addTabBar(x, y, totalWidth, labels, selectedIndex, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final TabBar addTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder
    ) {
        return controls.addTabBar(x, y, totalWidth, labels, tooltips, selectedIndex, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addButton( int x, int y, int width, Component text, Component tooltip, Runnable action ) {
        return controls.addButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addButton( int x, int y, int width, Component text, Component tooltip, Button.OnPress action ) {
        return controls.addButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addCompactButton( int x, int y, int width, Component text, Component tooltip, Runnable action ) {
        return controls.addCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Button addCompactButton( int x, int y, int width, Component text, Component tooltip, Button.OnPress action ) {
        return controls.addCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addHighZButton( int x, int y, int width, Component text, Component tooltip, int zLevel, Runnable action ) {
        return controls.addHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        return controls.addHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
    ) {
        return controls.addCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addCompactHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Button.OnPress action
    ) {
        return controls.addCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return controls.addToggleButton(x, y, width, value, onText, offText, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ToggleButton addToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder
    ) {
        return controls.addToggleButton(x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
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

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
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
        Button.OnPress onPress = ignored -> { if (action != null) action.run(); };
        HighZButton button = height == COMPACT_CONTROL_HEIGHT
                ? KineticWidgets.createCompactHighZButton(x, y, width, text, null, 0, onPress)
                : KineticWidgets.createHighZButton(x, y, width, text, null, 0, onPress);
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
    public final ColorSwatchButton addColorSwatchButton( int x, int y, int rgb, Component tooltip, Runnable action ) {
        return controls.addColorSwatchButton(x, y, rgb, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final ColorPreviewButton addColorPreviewButton(
            int x,
            int y,
            int width,
            int color,
            Component text,
            Component tooltip,
            Runnable action
    ) {
        return controls.addColorPreviewButton(x, y, width, color, text, tooltip, action);
    }

    public final void closeContextMenu() {
        controls.closeContextMenu();
    }

    /** 为控件登记 Screen Overlay Tooltip；null 或空文本移除登记，不修改控件自带 Tooltip。 */
    public final <T extends AbstractWidget> T registerWidgetTooltip(T widget, Component tooltip) {
        return controls.registerWidgetTooltip(widget, tooltip);
    }

    /** 注册已有控件并加入渲染和输入列表；非空 Tooltip 交由 Screen Overlay 显示。null Tooltip 保留控件自带 Tooltip。 */
    public final <T extends AbstractWidget> T addControl(T widget, Component tooltip) {
        return controls.addControl(widget, tooltip);
    }

    /** 移除控件及其 Screen Tooltip；固定画布 Screen 同时解除滚动视口绑定。 */
    public final void removeControl(AbstractWidget widget) {
        controls.removeControl(widget);
    }

    private void requestWidgetTooltip(double mouseX, double mouseY) {
        controls.requestWidgetTooltip(mouseX, mouseY);
    }

    /** 仅注册列表的输入事件；调用方负责通过对应 Screen 的列表渲染 API 绘制。 */
    public final <T extends ObjectSelectionList<?>> T addEventListWidget(T list) {
        return controls.addEventListWidget(list);
    }

    public final void resetScrollableWidgets() {
        scrollViewportWidgets.clear();
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
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

            enableUiScissor(
                    graphics,
                    viewportWidget.left(),
                    viewportWidget.top(),
                    viewportWidget.right(),
                    viewportWidget.bottom()
            );
            try {
                widget.render(graphics, mouseX, mouseY, partialTick);
            } finally {
                disableUiScissor(graphics);
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

    /** 设置草稿快照与恢复函数。离开共享草稿会话时回滚；capture 应返回独立且可按 equals 比较的快照。 */
    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, false);
    }

    /** 建立独立草稿保存边界，不继承父界面草稿；离开该边界时回滚未提交修改。 */
    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        draftSession.configureDraft(this, capture, restore, true);
    }

    /** 仅草稿所有者可更新已保存基线；先完成业务持久化，再调用本方法。本方法不写入配置。 */
    protected final void commitDraft() {
        draftSession.commitBaseline(this);
    }

    /** 恢复最近保存的草稿基线；不负责关闭界面。 */
    protected final void discardDraft() {
        draftSession.discardToBaseline();
    }

    /** 比较当前快照与保存基线，判断是否有未提交修改。 */
    protected final boolean hasUnsavedEdits() {
        return draftSession.isDirty();
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(Component component) {
        controls.showTooltip(component);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(List<? extends Component> lines) {
        controls.showTooltip(lines);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(Component component, int maxWidth) {
        controls.showTooltip(component, maxWidth);
    }

    /** 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。 */
    public final void showTooltip(List<? extends Component> lines, int maxWidth) {
        controls.showTooltip(lines, maxWidth);
    }

    /** 请求本帧的格式化 Tooltip，由统一 Overlay 渲染。 */
    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        controls.showFormattedTooltip(lines);
    }

    /** 请求本帧的物品 Tooltip，由统一 Overlay 渲染。 */
    public final void showItemTooltip(ItemStack stack) {
        controls.showItemTooltip(stack);
    }

    /** 转移焦点并清除旧控件的焦点状态；传 null 等同 clearControlFocus。 */
    public final void focusControl(GuiEventListener control) {
        focus.focusControl(control);
    }

    /** 清除指定控件的焦点；仅当它是当前焦点时解除 Screen 焦点。 */
    public final void blurControl(GuiEventListener control) {
        focus.blurControl(control);
    }

    /** 同时清除 Screen 当前焦点和该控件的焦点状态。 */
    public final void clearControlFocus() {
        focus.clearControlFocus();
    }

    /** 判断 Screen 当前焦点和控件自身焦点是否一致。 */
    public final boolean isControlFocused(GuiEventListener control) {
        return focus.isControlFocused(control);
    }

    /** 在当前 Screen 的 UI 坐标处打开统一菜单；Screen 负责坐标转换。 */
    public final void openContextMenu(double virtualX, double virtualY, List<GuiOverlay.MenuItem> items) {
        overlays.openMenu(toScreenX(virtualX), toScreenY(virtualY), items);
    }

    /** 打开统一模态确认框；保存或回滚动作由 onConfirm/onCancel 回调决定。 */
    public final void openDialog(
            Component title,
            Component message,
            Component confirmText,
            Component cancelText,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        controls.openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, selectedIndex, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, selectedIndex, tooltip, validator, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final Dropdown addDropdown(
            int x,
            int y,
            int width,
            List<? extends Component> options,
            List<? extends Component> optionTooltips,
            int selectedIndex,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return controls.addDropdown(x, y, width, options, optionTooltips, selectedIndex, tooltip, validator, responder);
    }

    @Override
    protected final void init() {
        super.init();
        updateMetrics();
        rebuildUi();
    }

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    public final void rebuildUi() {
        clearWidgets();
        controls.clear();
        scrollViewportWidgets.clear();
        buildUi();
    }

    protected abstract void buildUi();

    private void updateMetrics() {
        configureCanvas(STANDARD_CANVAS_WIDTH, STANDARD_CANVAS_HEIGHT, STANDARD_SAFE_MARGIN);
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
        enableUiScissor(graphics, left, top, right, bottom);
    }

    public final void disableCanvasScissor(GuiGraphics graphics) {
        disableUiScissor(graphics);
    }

    public final void renderTextFieldPlaceholder(GuiGraphics graphics, EditBox field, Component placeholder) {
        if (field == null || placeholder == null) return;
        if (field instanceof KineticEditBox kineticField) {
            kineticField.setPlaceholder(placeholder);
            return;
        }
        if (field.isFocused() || !field.getValue().isEmpty() || placeholder.getString().isBlank()) return;
        graphics.drawString(
                font,
                placeholder,
                field.getX() + 5,
                field.getY() + (field.getHeight() - font.lineHeight) / 2,
                GuiTheme.current().mutedText(),
                false
        );
    }

    /** 按当前 Screen 的 UI 坐标启用裁剪；与 disableUiScissor 配对使用。 */
    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof CanvasGuiGraphics) {
            graphics.enableScissor(left, top, right, bottom);
            return;
        }
        graphics.enableScissor(toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    /** 结束通过 enableUiScissor 开启的裁剪，建议放在 finally 中。 */
    public final void disableUiScissor(GuiGraphics graphics) {
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

    /** 返回父界面；离开共享草稿会话时回滚未提交修改，容器界面同时关闭容器。 */
    public final void navigateBack() {
        GuiSession.back(this);
    }

    @Override
    public void onClose() {
        navigateBack();
    }
}
