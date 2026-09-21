package dev.xyat.kineticcore.api.client.screen;

import javax.annotation.Nonnull;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import dev.xyat.kineticcore.internal.client.screen.KineticScreenControls;
import dev.xyat.kineticcore.internal.client.screen.KineticScreenFocus;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.TabBar;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTab;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableTabStrip;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.SelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridDensity;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemGridItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemGrid;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ItemActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableItemActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleActionItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleActionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.MultiToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleHit;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableMultiToggleList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableSelectionList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ToggleItem;
import dev.xyat.kineticcore.api.client.widget.selection.KineticTabs.ScrollableToggleList;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.api.client.widget.selection.KineticDropdowns.Option;
import dev.xyat.kineticcore.api.client.widget.slider.KineticSliders.Slider;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.CycleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.HighZToggleButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorPreviewButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ColorSwatchButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.ItemButton;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.api.client.widget.input.KineticAutoComplete;
import dev.xyat.kineticcore.api.client.layout.GuiLayout;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import dev.xyat.kineticcore.api.client.widget.KineticControl;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.api.client.widget.KineticWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Base screen for Kinetic interfaces using the fixed 640x360 virtual canvas.
 */
public abstract class KineticScreen extends Screen {
    /** Standard logical canvas width used by responsive Kinetic screens. */
    public static final int STANDARD_CANVAS_WIDTH = GuiLayout.MAX_CANVAS_WIDTH;
    /** Standard logical canvas height used by responsive Kinetic screens. */
    public static final int STANDARD_CANVAS_HEIGHT = GuiLayout.MAX_CANVAS_HEIGHT;
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

    private int canvasWidth = STANDARD_CANVAS_WIDTH;
    private int canvasHeight = STANDARD_CANVAS_HEIGHT;
    private int requestedCanvasWidth = STANDARD_CANVAS_WIDTH;
    private int requestedCanvasHeight = STANDARD_CANVAS_HEIGHT;
    private int requestedSafeMargin = STANDARD_SAFE_MARGIN;
    private float canvasScale;
    private int canvasX;
    private int canvasY;
    private GuiLayout.SafeArea safeArea = GuiLayout.SafeArea.of(1, 1, 0);
    private GuiLayout.Metrics metrics = GuiLayout.measure(1, 1, 640, 360);
    private final GuiOverlayRuntime overlays = new GuiOverlayRuntime();
    private final List<ScrollViewportWidget> scrollViewportWidgets = new ArrayList<>();
    private final KineticScreenControls controls = new KineticScreenControls(
            () -> font, overlays, this::addRenderableWidget, this::addWidget,
            widget -> { scrollViewportWidgets.removeIf(entry -> entry.widget() == widget); removeWidget(widget); }, this::openContextMenu
    );
    private final KineticScreenFocus focus = new KineticScreenFocus(this);

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
        KineticClientRuntimeImpl.initialize();
    }

    private void configureCanvas() {
        safeArea = GuiLayout.SafeArea.of(width, height, requestedSafeMargin);
        metrics = GuiLayout.measureCanvas(safeArea.width(), safeArea.height(), requestedCanvasWidth, requestedCanvasHeight);
        canvasScale = Math.max(0.0001f, metrics.fitScale());
        canvasWidth = requestedCanvasWidth;
        canvasHeight = requestedCanvasHeight;
        canvasX = safeArea.left() + Math.round((safeArea.width() - canvasWidth * canvasScale) / 2f);
        canvasY = safeArea.top() + Math.round((safeArea.height() - canvasHeight * canvasScale) / 2f);
    }

    /**
     * Selects the logical canvas used by this screen while keeping scaling, safe-area handling,
     * coordinate conversion, clipping, and high-DPI behavior owned by KineticCore.
     */
    protected final void useCanvas(float designWidth, float designHeight, int safeMargin) {
        requestedCanvasWidth = Math.max(1, Math.round(Float.isFinite(designWidth) ? designWidth : STANDARD_CANVAS_WIDTH));
        requestedCanvasHeight = Math.max(1, Math.round(Float.isFinite(designHeight) ? designHeight : STANDARD_CANVAS_HEIGHT));
        requestedSafeMargin = Math.max(0, safeMargin);
    }

    /** Returns the fixed virtual canvas width. */
    public final int canvasWidth() {
        return canvasWidth;
    }

    /** Returns the fixed virtual canvas height. */
    public final int canvasHeight() {
        return canvasHeight;
    }

    /** Returns the scale from virtual canvas coordinates to screen coordinates. */
    public final float canvasScale() {
        return canvasScale;
    }

    /** Returns the screen-space X origin of the virtual canvas. */
    public final int canvasX() {
        return canvasX;
    }

    /** Returns the screen-space Y origin of the virtual canvas. */
    public final int canvasY() {
        return canvasY;
    }

    /** Returns safe area. */
    public final GuiLayout.SafeArea safeArea() {
        return safeArea;
    }

    /** Returns layout. */
    public final GuiLayout.Metrics layout() {
        return metrics;
    }

    /** Returns layout level. */
    public final GuiLayout.Level layoutLevel() {
        return metrics.level();
    }

    /** Returns whether the current virtual canvas uses portrait layout rules. */
    public final boolean isPortraitLayout() {
        return metrics.isPortrait();
    }

    /** Returns whether the current virtual canvas uses ultrawide layout rules. */
    public final boolean isUltrawideLayout() {
        return metrics.isUltrawide();
    }

    /** Returns whether the current virtual canvas uses compact spacing rules. */
    public final boolean isCompactLayout() {
        return metrics.isCompact();
    }

    
    
    
    
    
    
    
    /** Creates a standard text field with no placeholder, validator, or tooltip. */
    public final KineticEditBox addTextField(int x, int y, int width, Component message) {
        return addTextField(x, y, width, message, null, null, null);
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
    public final KineticMultiLineEditBox addMultiLineTextField(
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
            Component placeholder,
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
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
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
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
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
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
            Supplier<List<KineticAutoComplete.Suggestion>> dictionarySupplier,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator,
            Component tooltip
    ) {
        return controls.addDecimalAutoCompleteField(x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    
    /**
     * 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。
     * Creates a numeric field without an attached tooltip.
     */
    public final NumericEditBox addIntegerField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Integer minValue,
            Integer maxValue,
            Predicate<Number> validator
    ) {
        return addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds an integer input using Kinetic validation, bounds, and standard styling. */
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

    
    /**
     * 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。
     * Creates a numeric field without an attached tooltip.
     */
    public final NumericEditBox addLongField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Long minValue,
            Long maxValue,
            Predicate<Number> validator
    ) {
        return addLongField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds a long-integer input using Kinetic validation, bounds, and standard styling. */
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

    
    /**
     * 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。
     * Creates a numeric field without an attached tooltip.
     */
    public final NumericEditBox addDecimalField(
            int x,
            int y,
            int width,
            Component message,
            boolean allowNegative,
            Double minValue,
            Double maxValue,
            Predicate<Number> validator
    ) {
        return addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds a decimal input using Kinetic validation, bounds, and standard styling. */
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

    


    /** Creates and registers a smooth vertical single-selection list using standard Kinetic row controls. */
    public final ScrollableSelectionList addScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return controls.addScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder
        );
    }

    /** Creates and registers a smooth vertical single-selection list rendered at the supplied Z depth. */
    public final ScrollableSelectionList addHighZScrollableSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return controls.addHighZScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates and registers a smooth vertical item-backed single-selection list. */
    public final ScrollableItemSelectionList addScrollableItemSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder
    ) {
        return controls.addScrollableItemSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder
        );
    }

    /** Creates and registers a smooth vertical item-backed single-selection list rendered at the supplied Z depth. */
    public final ScrollableItemSelectionList addHighZScrollableItemSelectionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return controls.addHighZScrollableItemSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates and registers a smooth scrollable item-slot grid. */
    public final ScrollableItemGrid addScrollableItemGrid(
            int x, int y, int width, int height,
            ItemGridDensity density, List<? extends ItemGridItem> items,
            int initialScrollOffset, Consumer<Integer> responder
    ) {
        return controls.addScrollableItemGrid(
                x, y, width, height, density, items, initialScrollOffset, responder
        );
    }

    /** Creates and registers a smooth scrollable item-slot grid rendered at the supplied Z depth. */
    public final ScrollableItemGrid addHighZScrollableItemGrid(
            int x, int y, int width, int height,
            ItemGridDensity density, List<? extends ItemGridItem> items,
            int initialScrollOffset, Consumer<Integer> responder, int zLevel
    ) {
        return controls.addHighZScrollableItemGrid(
                x, y, width, height, density, items, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates and registers a smooth vertical single-selection list with one trailing row action. */
    public final ScrollableActionList addScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        return controls.addScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder
        );
    }

    /** Creates and registers a smooth vertical single-selection list with one trailing row action at the supplied Z depth. */
    public final ScrollableActionList addHighZScrollableActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return controls.addHighZScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates and registers a smooth vertical single-selection list with multiple trailing row actions. */
    public final ScrollableMultiActionList addScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder
    ) {
        return controls.addScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder
        );
    }

    /** Creates and registers a smooth vertical single-selection list with multiple trailing row actions at the supplied Z depth. */
    public final ScrollableMultiActionList addHighZScrollableMultiActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder,
            int zLevel
    ) {
        return controls.addHighZScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, actionResponder, zLevel
        );
    }

    /** Creates and registers a smooth vertical single-selection list with one real toggle and one trailing row action. */
    public final ScrollableToggleActionList addScrollableToggleActionList(
            int x, int y, int width, int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex, int initialScrollOffset,
            int toggleWidth, int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder
    ) {
        return controls.addScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder
        );
    }

    /** Creates and registers a smooth vertical single-selection list with one real toggle and one trailing row action at the supplied Z depth. */
    public final ScrollableToggleActionList addHighZScrollableToggleActionList(
            int x, int y, int width, int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex, int initialScrollOffset,
            int toggleWidth, int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return controls.addHighZScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, zLevel
        );
    }

    /** Creates and registers a smooth vertical item-backed single-selection list with one trailing row action. */
    public final ScrollableItemActionList addScrollableItemActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder
    ) {
        return controls.addScrollableItemActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder
        );
    }

    /** Creates and registers a smooth vertical item-backed single-selection list with one trailing row action at the supplied Z depth. */
    public final ScrollableItemActionList addHighZScrollableItemActionList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        return controls.addHighZScrollableItemActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel
        );
    }

    /** Creates and registers a smooth vertical single-selection list with any number of real toggles per row. */
    public final ScrollableMultiToggleList addScrollableMultiToggleList(
            int x, int y, int width, int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex, int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder
    ) {
        return controls.addScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, toggleResponder
        );
    }

    /** Creates and registers a smooth vertical single-selection list with any number of real toggles per row at the supplied Z depth. */
    public final ScrollableMultiToggleList addHighZScrollableMultiToggleList(
            int x, int y, int width, int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex, int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder,
            int zLevel
    ) {
        return controls.addHighZScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                responder, toggleResponder, zLevel
        );
    }

    /** Creates and registers a smooth vertical multi-toggle list using standard Kinetic row controls. */
    public final ScrollableToggleList addScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder
    ) {
        return controls.addScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder
        );
    }

    /** Creates and registers a smooth vertical multi-toggle list rendered at the supplied Z depth. */
    public final ScrollableToggleList addHighZScrollableToggleList(
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder,
            int zLevel
    ) {
        return controls.addHighZScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder, zLevel
        );
    }

    /** Creates and registers a compact variable-width tab strip with API-managed horizontal scrolling. */
    public final ScrollableTabStrip addScrollableTabStrip(
            int x,
            int y,
            int width,
            List<? extends ScrollableTab> tabs,
            int pinnedLeadingTabs,
            int selectedIndex,
            int initialScrollOffset,
            Component previousText,
            Component nextText,
            Consumer<Integer> responder
    ) {
        return controls.addScrollableTabStrip(
                x, y, width, tabs, pinnedLeadingTabs, selectedIndex, initialScrollOffset,
                previousText, nextText, responder
        );
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

    
    /** Creates and registers a tab bar rendered at an elevated Z depth. */
    public final TabBar addHighZTabBar(
            int x,
            int y,
            int totalWidth,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return controls.addHighZTabBar(x, y, totalWidth, labels, tooltips, selectedIndex, responder, zLevel);
    }

    
    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final StateButton addButton( int x, int y, int width, Component text, Component tooltip, Runnable action ) {
        return controls.addButton(x, y, width, text, tooltip, action);
    }

    /** Creates and registers a content-rich card button using the API-defined card height. */
    public final StateButton addCardButton(int x, int y, int width, Component narration, Component tooltip, Runnable action) {
        return controls.addCardButton(x, y, width, narration, tooltip, action);
    }

    
    /** Creates and registers a standard item-backed button with API-managed height, rendering, and tooltip. */
    public final ItemButton addItemButton(
            int x, int y, int width, ItemStack icon, Component text, Component tooltip, Runnable action
    ) {
        return controls.addItemButton(x, y, width, icon, text, tooltip, action);
    }

    /** Creates and registers a managed state button whose callback needs that button instance. */
    public final StateButton addButtonWithHandler(
            int x, int y, int width, Component text, Component tooltip, Consumer<StateButton> action
    ) {
        return controls.addButtonWithHandler(x, y, width, text, tooltip, action);
    }

    /** Creates and registers a vertical tab bar rendered at an elevated Z depth. */
    public final TabBar addVerticalHighZTabBar(
            int x,
            int y,
            int width,
            List<? extends Component> labels,
            List<? extends Component> tooltips,
            int selectedIndex,
            Consumer<Integer> responder,
            int zLevel
    ) {
        return controls.addVerticalHighZTabBar(x, y, width, labels, tooltips, selectedIndex, responder, zLevel);
    }



    /** Creates and registers a standard Kinetic slider with caller-defined business rules. */
    public final Slider addSlider(
            int x,
            int y,
            int width,
            Component message,
            double minValue,
            double maxValue,
            double step,
            double value,
            Predicate<Double> validator,
            DoubleConsumer responder,
            Component tooltip
    ) {
        return controls.addSlider(
                x, y, width, message, minValue, maxValue, step, value, validator, responder, tooltip
        );
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final StateButton addCompactButton( int x, int y, int width, Component text, Component tooltip, Runnable action ) {
        return controls.addCompactButton(x, y, width, text, tooltip, action);
    }

    
    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    public final HighZButton addHighZButton(
            int x,
            int y,
            int width,
            Component text,
            Component tooltip,
            int zLevel,
            Runnable action
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

    
    /** Creates and registers a toggle control without a custom validator. */
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
        return addToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
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

    /**
     * Creates and registers a compact-height Kinetic toggle control.
     * Creates and registers a compact toggle control without a custom validator.
     */
    public final ToggleButton addCompactToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Consumer<Boolean> responder
    ) {
        return addCompactToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
    }

    /** Adds a compact themed toggle button using the standard Kinetic control height. */
    public final ToggleButton addCompactToggleButton(
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
        return controls.addCompactToggleButton(x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** Creates and registers a standard Kinetic multi-state cycle control. */
    public final CycleButton addCycleButton(
            int x,
            int y,
            int width,
            int index,
            List<Component> options,
            Component tooltip,
            Predicate<Integer> validator,
            Consumer<Integer> responder
    ) {
        return controls.addCycleButton(x, y, width, index, options, tooltip, validator, responder);
    }

    /** 创建并注册高层 Kinetic Toggle，统一处理模态层级、状态和 Tooltip。 */
    public final HighZToggleButton addHighZToggleButton(
            int x,
            int y,
            int width,
            boolean value,
            Component onText,
            Component offText,
            Component tooltip,
            Predicate<Boolean> validator,
            Consumer<Boolean> responder,
            int zLevel
    ) {
        return controls.addHighZToggleButton(
                x, y, width, value, onText, offText, tooltip, validator, responder, zLevel
        );
    }

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

    /** Returns whether a menu or modal dialog currently blocks this screen's underlying input and hover content. */
    protected final boolean overlayBlocksInput() {
        return overlays.blocksInput();
    }

    /** Closes the active standard context menu, if one is open. */
    public final void closeContextMenu() {
        controls.closeContextMenu();
    }

    /** Registers Screen Overlay Tooltip state for one standard Kinetic control. */
    public final <T extends KineticControl> T registerWidgetTooltip(T control, Component tooltip) {
        if (control == null) return null;
        controls.registerWidgetTooltip(KineticControlBridge.widget(control), tooltip);
        return control;
    }

    /** Registers a dynamic Screen Overlay Tooltip evaluated at hover time for one standard Kinetic control. */
    public final <T extends KineticControl> T registerDynamicWidgetTooltip(T control, Supplier<Component> tooltipSupplier) {
        if (control == null) return null;
        controls.registerDynamicWidgetTooltip(KineticControlBridge.widget(control), tooltipSupplier);
        return control;
    }

    /** Registers an API-created control for rendering, input, and a single screen-managed tooltip. */
    public final <T extends KineticControl> T addControl(T control, Component tooltip) {
        if (control == null) return null;
        controls.registerWidget(KineticControlBridge.widget(control), tooltip);
        return control;
    }

    /** Registers one business-specific or vanilla-special external widget that has no standard Kinetic control equivalent. */
    public final <T extends AbstractWidget> T addExternalWidget(T widget, Component tooltip) {
        return controls.registerWidget(widget, tooltip);
    }

    /** Removes one widget previously registered through {@link #addExternalWidget(AbstractWidget, Component)}. */
    public final void removeExternalWidget(AbstractWidget widget) {
        focus.blurControl(widget);
        controls.unregisterWidget(widget);
    }

    /** Removes one standard Kinetic control without exposing Minecraft widget types to addons. */
    public final void removeKineticControl(KineticControl control) {
        if (control == null) return;
        AbstractWidget widget = KineticControlBridge.widget(control);
        blurControl(control);
        controls.unregisterWidget(widget);
    }

    private boolean requestWidgetTooltip(double mouseX, double mouseY) {
        return controls.requestWidgetTooltip(
                mouseX,
                mouseY,
                widget -> isWidgetTooltipHitAllowed(widget, mouseX, mouseY)
        );
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

    /** 仅注册列表的输入事件；调用方负责通过对应 Screen 的列表渲染 API 绘制。 */
    public final <T extends SmoothSelectionList<?>> T addSmoothSelectionList(T list) {
        return controls.addSmoothSelectionList(list);
    }

    /** Resets scrollable widgets. */
    public final void resetScrollableWidgets() {
        scrollViewportWidgets.clear();
    }

    /**
     * Adds or reuses one widget in a Kinetic scrolling viewport. Already registered widgets are not
     * registered a second time, and rebinding the same widget replaces its previous viewport binding.
     */
    public final <T extends KineticControl> T addScrollableWidget(
            T control,
            int left,
            int top,
            int right,
            int bottom,
            DoubleSupplier pixelOffset
    ) {
        if (control == null) return null;
        AbstractWidget widget = KineticControlBridge.widget(control);
        if (!children().contains(widget)) controls.registerWidget(widget, null);
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
            if (focus.isControlFocused(widget)
                    && (widgetBottom <= viewportWidget.top() || widgetTop >= viewportWidget.bottom())) {
                focus.blurControl(widget);
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
                widget.render(graphics, mouseX, mouseY, partialTick);
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

    /** 设置草稿快照与恢复函数。离开共享草稿会话时回滚；capture 应返回独立且可按 equals 比较的快照。 */
    protected final <T> void configureDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        GuiSessionRuntime.configureDraft(this, capture, restore, false);
    }

    /** 建立独立草稿保存边界，不继承父界面草稿；离开该边界时回滚未提交修改。 */
    protected final <T> void configureStandaloneDraft(java.util.function.Supplier<T> capture, java.util.function.Consumer<T> restore) {
        GuiSessionRuntime.configureDraft(this, capture, restore, true);
    }

    /** 仅草稿所有者可更新已保存基线；先完成业务持久化，再调用本方法。本方法不写入配置。 */
    protected final void commitDraft() {
        GuiSessionRuntime.commitDraft(this);
    }

    /** 恢复最近保存的草稿基线；不负责关闭界面。 */
    protected final void discardDraft() {
        GuiSessionRuntime.discardDraft(this);
    }

    /** 比较当前快照与保存基线，判断是否有未提交修改。 */
    protected final boolean hasUnsavedEdits() {
        return GuiSessionRuntime.hasUnsavedEdits(this);
    }

    /**
     * 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。
     * Requests this frame's standard one-line tooltip without restoring a same-name overload.
     */
    public final void showTooltipLine(Component component) {
        controls.showTooltip(List.of(component == null ? Component.empty() : component), null);
    }

    /** Requests this frame's standard tooltip. Null maxWidth keeps text unwrapped. */
    public final void showTooltip(List<? extends Component> lines, Integer maxWidth) {
        controls.showTooltip(lines, maxWidth);
    }

    /** Shows formatted tooltip lines through the screen overlay layer. */
    public final void showFormattedTooltip(List<FormattedCharSequence> lines) {
        controls.showFormattedTooltip(lines);
    }

    /** 请求本帧的物品 Tooltip，由统一 Overlay 渲染。 */
    public final void showItemTooltip(ItemStack stack) {
        controls.showItemTooltip(stack);
    }

    /** 转移焦点并清除旧控件的焦点状态；传 null 等同 clearControlFocus。 */
    public final void focusControl(KineticControl control) {
        focus.focusControl(control == null ? null : KineticControlBridge.widget(control));
    }

    /** 清除指定 Kinetic 控件的焦点；仅当它是当前焦点时解除 Screen 焦点。 */
    public final void blurControl(KineticControl control) {
        if (control == null) return;
        focus.blurControl(KineticControlBridge.widget(control));
    }

    /** 同时清除 Screen 当前焦点和该控件的焦点状态。 */
    public final void clearControlFocus() {
        focus.clearControlFocus();
    }

    /** 判断 Screen 当前焦点和 Kinetic 控件自身焦点是否一致。 */
    public final boolean isControlFocused(KineticControl control) {
        return control != null && focus.isControlFocused(KineticControlBridge.widget(control));
    }

    /** Returns the currently focused standard Kinetic control, or {@code null} when focus belongs elsewhere. */
    public final KineticControl focusedControl() {
        return getFocused() instanceof KineticControl control ? control : null;
    }

    /** 在当前 Screen 的 UI 坐标处打开统一菜单；Screen 负责坐标转换。 */
    public final void openContextMenu(double virtualX, double virtualY, List<KineticOverlays.MenuItem> items) {
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
            List<? extends Option> options,
            String selectedValue,
            Component tooltip,
            Predicate<String> validator,
            Consumer<String> responder
    ) {
        return controls.addDropdown(x, y, width, options, selectedValue, tooltip, validator, responder);
    }

    @Override
    protected final void init() {
        super.init();
        updateMetrics();
        rebuildUi();
    }

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    public final void rebuildUi() {
        clearControlFocus();
        closeContextMenu();
        clearWidgets();
        controls.clear();
        scrollViewportWidgets.clear();
        buildUi();
    }

    /** Builds this screen's API-managed controls for the current UI rebuild. */
    protected abstract void buildUi();

    private void updateMetrics() {
        configureCanvas();
    }

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
    @Override
    public final void tick() {
        super.tick();
        controls.tickManagedControls();
        canvasTick();
        focus.synchronizeControlState();
    }

    /** Handles business per-tick work after standard registered controls have ticked. */
    protected void canvasTick() {
    }

    /** {@inheritDoc} */
    @Override
    public final void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        focus.synchronizeControlState();
        renderBackground(graphics);
        overlays.beginFrame();

        int virtualMouseX = (int) Math.floor(toVirtualX(mouseX));
        int virtualMouseY = (int) Math.floor(toVirtualY(mouseY));
        CanvasGuiGraphics canvasGraphics = new CanvasGuiGraphics(graphics);
        canvasGraphics.pose().pushPose();
        canvasGraphics.pose().translate(canvasX, canvasY, 0);
        canvasGraphics.pose().scale(canvasScale, canvasScale, 1f);
        canvasGraphics.enableScissor(0, 0, canvasWidth, canvasHeight);
        try {
            updateScrollableWidgetPositions();
            renderCanvasBackground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);

            List<AbstractWidget> hiddenScrollableWidgets = hideScrollableWidgetsForDefaultRender();
            try {
                super.render(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
            } finally {
                restoreScrollableWidgetVisibility(hiddenScrollableWidgets);
            }

            renderScrollableWidgets(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
            renderCanvasForeground(canvasGraphics, virtualMouseX, virtualMouseY, partialTick);
            controls.renderAutoCompleteSuggestions(canvasGraphics, virtualMouseX, virtualMouseY);

            if (isInsideCanvas(mouseX, mouseY)
                    && !overlays.blocksInput()
                    && !controls.hasOpenAutoCompletePopup()
                    && !requestWidgetTooltip(virtualMouseX, virtualMouseY)) {
                renderTooltips(canvasGraphics, virtualMouseX, virtualMouseY, mouseX, mouseY);
            }
        } finally {
            try {
                canvasGraphics.disableScissor();
            } finally {
                canvasGraphics.pose().popPose();
            }
        }

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

    /** Converts the supplied value to virtual x. */
    public final double toVirtualX(double screenX) {
        return (screenX - canvasX) / canvasScale;
    }

    /** Converts the supplied value to virtual y. */
    public final double toVirtualY(double screenY) {
        return (screenY - canvasY) / canvasScale;
    }

    /** Converts the supplied value to screen x. */
    public final int toScreenX(double virtualX) {
        return canvasX + (int) Math.floor(virtualX * canvasScale);
    }

    /** Converts the supplied value to screen y. */
    public final int toScreenY(double virtualY) {
        return canvasY + (int) Math.floor(virtualY * canvasScale);
    }

    /** Converts the supplied value to screen right. */
    public final int toScreenRight(double virtualX) {
        return canvasX + (int) Math.ceil(virtualX * canvasScale);
    }

    /** Converts the supplied value to screen bottom. */
    public final int toScreenBottom(double virtualY) {
        return canvasY + (int) Math.ceil(virtualY * canvasScale);
    }

    /** Returns whether the supplied screen-space point lies inside the Kinetic canvas. */
    public final boolean isInsideCanvas(double screenX, double screenY) {
        double virtualX = toVirtualX(screenX);
        double virtualY = toVirtualY(screenY);
        return virtualX >= 0 && virtualX < canvasWidth && virtualY >= 0 && virtualY < canvasHeight;
    }

    /** 按当前 Screen 的 UI 坐标启用裁剪；与 disableUiScissor 配对使用。 */
    public final void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        if (graphics instanceof CanvasGuiGraphics) {
            KineticRenderRuntime.enableScissor(graphics, left, top, right, bottom);
            return;
        }
        KineticRenderRuntime.enableScissor(graphics, toScreenX(left), toScreenY(top), toScreenRight(right), toScreenBottom(bottom));
    }

    /** 结束通过 enableUiScissor 开启的裁剪，建议放在 finally 中。 */
    public final void disableUiScissor(GuiGraphics graphics) {
        KineticRenderRuntime.disableScissor(graphics);
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

    /** Renders one Kinetic smooth selection list using this Screen's responsive canvas transform. */
    public final void renderSmoothSelectionList(
            SmoothSelectionList<?> list,
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

    /** {@inheritDoc} */
    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (overlays.mouseClicked(mouseX, mouseY, button, width, height, font)) {
            return true;
        }
        if (!isInsideCanvas(mouseX, mouseY)) {
            controls.clearAutoCompleteFocusOutside(virtualMouseX, virtualMouseY);
            clearControlFocus();
            return false;
        }
        controls.clearAutoCompleteFocusOutside(virtualMouseX, virtualMouseY);
        if (controls.handleAutoCompleteClick(virtualMouseX, virtualMouseY, button)) {
            return true;
        }
        boolean handled = canvasMouseClicked(virtualMouseX, virtualMouseY, button);
        if (!handled) {
            clearControlFocus();
        }
        return handled;
    }

    /** Handles a mouse click after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        updateScrollableWidgetPositions();
        List<AbstractWidget> hidden = hideScrollableWidgetsOutsideViewport(mouseX, mouseY);
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            restoreScrollableWidgetVisibility(hidden);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (overlays.blocksInput()) {
            return true;
        }
        if (controls.handleAutoCompleteReleased(button)) return true;
        return canvasMouseReleased(toVirtualX(mouseX), toVirtualY(mouseY), button);
    }

    /** Handles mouse release after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overlays.blocksInput()) return true;
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (controls.handleAutoCompleteDragged(virtualMouseX, virtualMouseY)) return true;
        return canvasMouseDragged(
                virtualMouseX,
                virtualMouseY,
                button,
                dragX / canvasScale,
                dragY / canvasScale
        );
    }

    /** Handles mouse dragging after converting screen coordinates to virtual canvas coordinates. */
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overlays.blocksInput()) return true;
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        if (controls.handleAutoCompleteScroll(virtualMouseX, virtualMouseY, delta)) return true;
        return canvasMouseScrolled(virtualMouseX, virtualMouseY, delta);
    }

    /** Handles mouse scrolling in virtual canvas coordinates. */
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (GuiSessionRuntime.routeSelectionListWheel(children(), mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** {@inheritDoc} */
    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        if (overlays.blocksInput()) return;
        double virtualMouseX = toVirtualX(mouseX);
        double virtualMouseY = toVirtualY(mouseY);
        canvasMouseMoved(virtualMouseX, virtualMouseY);
        super.mouseMoved(virtualMouseX, virtualMouseY);
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
    @Override
    public final boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        focus.synchronizeControlState();
        if (overlays.keyPressed(keyCode)) return true;
        if (controls.handleAutoCompleteKey(keyCode, scanCode, modifiers)) return true;
        if (canvasKeyPressed(keyCode, scanCode, modifiers)) return true;
        if (KineticKeyBindings.matchesKeyCode(KineticKeyBindings.Key.ESCAPE, keyCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        focus.synchronizeControlState();
        if (overlays.blocksInput()) return true;
        if (canvasKeyReleased(keyCode, scanCode, modifiers)) return true;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean charTyped(char codePoint, int modifiers) {
        focus.synchronizeControlState();
        if (overlays.blocksInput()) return true;
        if (canvasCharTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    /** Explicitly declares the parent used by standard Kinetic back navigation. */
    protected final void setParentScreen(Screen parent) {
        GuiSessionRuntime.setExplicitParent(this, parent);
    }

    /** 返回父界面；离开共享草稿会话时回滚未提交修改，容器界面同时关闭容器。 */
    public final void navigateBack() {
        GuiSessionRuntime.back(this);
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
}
