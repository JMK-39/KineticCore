package dev.xyat.kineticcore.internal.client.gui.screen;

import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown.Option;
import dev.xyat.kineticcore.api.client.gui.widget.KineticTabStrip;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.*;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.AutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticAutoComplete.NumericAutoCompleteBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticNumericFields.NumericEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticMultiLineEditBox;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.SmoothSelectionList;
import dev.xyat.kineticcore.internal.client.gui.widget.selection.KineticDropdowns.Dropdown;
import dev.xyat.kineticcore.internal.client.gui.widget.slider.KineticSliders.Slider;
import dev.xyat.kineticcore.internal.client.gui.widget.tab.TabBarButtons;
import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 三种 Kinetic 界面宿主的共享控件、Overlay、焦点、草稿与坐标 API 的唯一实现。
 * <p>
 * The one implementation of the control, overlay, focus, draft and coordinate API shared by {@link KineticScreen},
 * {@link KineticContainerScreen} and {@link KineticNativeScreen}. Every method here delegates to the host's
 * {@link KineticScreenRuntime}; host screens implement only {@link #screen()}, {@link #kineticRuntime()}, the page
 * size and {@link #rebuildUi()}, and must not redeclare these defaults ({@code ScreenApiParityRegression} enforces
 * that). Page hosts and the page UI builder program against this interface.
 */
public interface KineticScreenHost {
    /** Returns the vanilla screen implementing this host. */
    Screen screen();

    /** Returns the shared runtime behind this host. */
    KineticScreenRuntime kineticRuntime();

    //? if >=1.20.2 {
    /*// Draws the vanilla screen background once per frame, outside the canvas transform. Screen.render draws it
    // itself since 1.20.2, so host screens turn that call off and KineticScreenRuntime calls this instead.
    void renderVanillaBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    *///?}

    /** Width of the host's page coordinate space. */
    int pageWidth();

    /** Height of the host's page coordinate space. */
    int pageHeight();

    /** 通过 buildUi 重建并重新注册控件，清理旧 Tooltip 和视口绑定；不要直接调用 this.init() 或 clearWidgets。 */
    void rebuildUi();

    /** Routes a close request through the host's close hook and standard back navigation. */
    void onClose();

    // ---- coordinates -------------------------------------------------------------------------------------------

    /** Converts a screen X coordinate to page coordinates. */
    default double toVirtualX(double screenX) {
        return kineticRuntime().canvas().toVirtualX(screenX);
    }

    /** Converts a screen Y coordinate to page coordinates. */
    default double toVirtualY(double screenY) {
        return kineticRuntime().canvas().toVirtualY(screenY);
    }

    /** Converts a page X coordinate to screen coordinates. */
    default int toScreenX(double virtualX) {
        return kineticRuntime().canvas().toScreenX(virtualX);
    }

    /** Converts a page Y coordinate to screen coordinates. */
    default int toScreenY(double virtualY) {
        return kineticRuntime().canvas().toScreenY(virtualY);
    }

    /** Converts an exclusive page right edge to screen coordinates. */
    default int toScreenRight(double virtualX) {
        return kineticRuntime().canvas().toScreenRight(virtualX);
    }

    /** Converts an exclusive page bottom edge to screen coordinates. */
    default int toScreenBottom(double virtualY) {
        return kineticRuntime().canvas().toScreenBottom(virtualY);
    }

    /** 按当前 Screen 的 UI 坐标启用裁剪；与 disableUiScissor 配对使用。 */
    default void enableUiScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        kineticRuntime().enableUiScissor(graphics, left, top, right, bottom);
    }

    /** 结束通过 enableUiScissor 开启的裁剪，建议放在 finally 中。 */
    default void disableUiScissor(GuiGraphics graphics) {
        kineticRuntime().disableUiScissor(graphics);
    }

    // ---- text input ---------------------------------------------------------------------------------------------

    /** Creates a standard text field with no placeholder, validator, or tooltip. */
    default KineticEditBox addTextField(int x, int y, int width, Component message) {
        return addTextField(x, y, width, message, null, null, null);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default KineticEditBox addTextField(int x, int y, int width, Component message, Component placeholder,
                                        Predicate<String> validator, Component tooltip) {
        return kineticRuntime().controls().addTextField(x, y, width, message, placeholder, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default KineticMultiLineEditBox addMultiLineTextField(int x, int y, int width, int height, Component message,
                                                          Component placeholder, Component tooltip) {
        return kineticRuntime().controls().addMultiLineTextField(x, y, width, height, message, placeholder, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default AutoCompleteBox addAutoCompleteField(int x, int y, int width, Component message, Component placeholder,
                                                 Supplier<List<KineticSuggestion>> dictionarySupplier, Component tooltip) {
        return kineticRuntime().controls().addAutoCompleteField(x, y, width, message, placeholder, dictionarySupplier, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default NumericAutoCompleteBox addIntegerAutoCompleteField(int x, int y, int width, Component message,
                                                               Supplier<List<KineticSuggestion>> dictionarySupplier,
                                                               boolean allowNegative, Integer minValue, Integer maxValue,
                                                               Predicate<Number> validator, Component tooltip) {
        return kineticRuntime().controls().addIntegerAutoCompleteField(
                x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default NumericAutoCompleteBox addLongAutoCompleteField(int x, int y, int width, Component message,
                                                            Supplier<List<KineticSuggestion>> dictionarySupplier,
                                                            boolean allowNegative, Long minValue, Long maxValue,
                                                            Predicate<Number> validator, Component tooltip) {
        return kineticRuntime().controls().addLongAutoCompleteField(
                x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default NumericAutoCompleteBox addDecimalAutoCompleteField(int x, int y, int width, Component message,
                                                               Supplier<List<KineticSuggestion>> dictionarySupplier,
                                                               boolean allowNegative, Double minValue, Double maxValue,
                                                               Predicate<Number> validator, Component tooltip) {
        return kineticRuntime().controls().addDecimalAutoCompleteField(
                x, y, width, message, dictionarySupplier, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** Creates an integer field without an attached tooltip. */
    default NumericEditBox addIntegerField(int x, int y, int width, Component message, boolean allowNegative,
                                           Integer minValue, Integer maxValue, Predicate<Number> validator) {
        return addIntegerField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds an integer input using Kinetic validation, bounds, and standard styling. */
    default NumericEditBox addIntegerField(int x, int y, int width, Component message, boolean allowNegative,
                                           Integer minValue, Integer maxValue, Predicate<Number> validator,
                                           Component tooltip) {
        return kineticRuntime().controls().addIntegerField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** Creates a long-integer field without an attached tooltip. */
    default NumericEditBox addLongField(int x, int y, int width, Component message, boolean allowNegative,
                                        Long minValue, Long maxValue, Predicate<Number> validator) {
        return addLongField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds a long-integer input using Kinetic validation, bounds, and standard styling. */
    default NumericEditBox addLongField(int x, int y, int width, Component message, boolean allowNegative,
                                        Long minValue, Long maxValue, Predicate<Number> validator, Component tooltip) {
        return kineticRuntime().controls().addLongField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    /** Creates a decimal field without an attached tooltip. */
    default NumericEditBox addDecimalField(int x, int y, int width, Component message, boolean allowNegative,
                                           Double minValue, Double maxValue, Predicate<Number> validator) {
        return addDecimalField(x, y, width, message, allowNegative, minValue, maxValue, validator, null);
    }

    /** Adds a decimal input using Kinetic validation, bounds, and standard styling. */
    default NumericEditBox addDecimalField(int x, int y, int width, Component message, boolean allowNegative,
                                           Double minValue, Double maxValue, Predicate<Number> validator,
                                           Component tooltip) {
        return kineticRuntime().controls().addDecimalField(
                x, y, width, message, allowNegative, minValue, maxValue, validator, tooltip);
    }

    // ---- lists and tabs -----------------------------------------------------------------------------------------

    /** Creates and registers a smooth vertical single-selection list using standard Kinetic row controls. */
    default KineticSelectionList addScrollableSelectionList(int x, int y, int width, int height,
                                                            List<? extends SelectionItem> items, int selectedIndex,
                                                            int initialScrollOffset, Consumer<Integer> responder) {
        return kineticRuntime().controls().addScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder);
    }

    /** Creates and registers a smooth vertical single-selection list rendered at the supplied Z depth. */
    default KineticSelectionList addHighZScrollableSelectionList(int x, int y, int width, int height,
                                                                 List<? extends SelectionItem> items, int selectedIndex,
                                                                 int initialScrollOffset, Consumer<Integer> responder,
                                                                 int zLevel) {
        return kineticRuntime().controls().addHighZScrollableSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel);
    }

    /** Creates and registers a smooth vertical item-backed single-selection list. */
    default KineticItemSelectionList addScrollableItemSelectionList(int x, int y, int width, int height,
                                                                    List<? extends ItemSelectionItem> items,
                                                                    int selectedIndex, int initialScrollOffset,
                                                                    Consumer<Integer> responder) {
        return kineticRuntime().controls().addScrollableItemSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder);
    }

    /** Creates and registers a smooth vertical item-backed single-selection list rendered at the supplied Z depth. */
    default KineticItemSelectionList addHighZScrollableItemSelectionList(int x, int y, int width, int height,
                                                                         List<? extends ItemSelectionItem> items,
                                                                         int selectedIndex, int initialScrollOffset,
                                                                         Consumer<Integer> responder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableItemSelectionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, zLevel);
    }

    /** Creates and registers a smooth scrollable item-slot grid. */
    default KineticItemGrid addScrollableItemGrid(int x, int y, int width, int height, ItemGridDensity density,
                                                  List<? extends ItemGridItem> items, int initialScrollOffset,
                                                  Consumer<Integer> responder) {
        return kineticRuntime().controls().addScrollableItemGrid(
                x, y, width, height, density, items, initialScrollOffset, responder);
    }

    /** Creates and registers a smooth scrollable item-slot grid rendered at the supplied Z depth. */
    default KineticItemGrid addHighZScrollableItemGrid(int x, int y, int width, int height, ItemGridDensity density,
                                                       List<? extends ItemGridItem> items, int initialScrollOffset,
                                                       Consumer<Integer> responder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableItemGrid(
                x, y, width, height, density, items, initialScrollOffset, responder, zLevel);
    }

    /** Creates and registers a smooth vertical single-selection list with one trailing row action. */
    default KineticActionList addScrollableActionList(int x, int y, int width, int height,
                                                      List<? extends ActionItem> items, int selectedIndex,
                                                      int initialScrollOffset, int actionWidth,
                                                      Consumer<Integer> responder, Consumer<Integer> actionResponder) {
        return kineticRuntime().controls().addScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, actionWidth, responder, actionResponder);
    }

    /** Creates and registers a smooth vertical single-selection list with one trailing row action at the supplied Z depth. */
    default KineticActionList addHighZScrollableActionList(int x, int y, int width, int height,
                                                           List<? extends ActionItem> items, int selectedIndex,
                                                           int initialScrollOffset, int actionWidth,
                                                           Consumer<Integer> responder,
                                                           Consumer<Integer> actionResponder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel);
    }

    /** Creates and registers a smooth vertical single-selection list with multiple trailing row actions. */
    default KineticMultiActionList addScrollableMultiActionList(int x, int y, int width, int height,
                                                                List<? extends MultiActionItem> items,
                                                                int selectedIndex, int initialScrollOffset,
                                                                Consumer<Integer> responder,
                                                                BiConsumer<Integer, Integer> actionResponder) {
        return kineticRuntime().controls().addScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, actionResponder);
    }

    /** Creates and registers a smooth vertical single-selection list with multiple trailing row actions at the supplied Z depth. */
    default KineticMultiActionList addHighZScrollableMultiActionList(int x, int y, int width, int height,
                                                                     List<? extends MultiActionItem> items,
                                                                     int selectedIndex, int initialScrollOffset,
                                                                     Consumer<Integer> responder,
                                                                     BiConsumer<Integer, Integer> actionResponder,
                                                                     int zLevel) {
        return kineticRuntime().controls().addHighZScrollableMultiActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, actionResponder, zLevel);
    }

    /** Creates and registers a smooth vertical single-selection list with one real toggle and one trailing row action. */
    default KineticToggleActionList addScrollableToggleActionList(int x, int y, int width, int height,
                                                                  List<? extends ToggleActionItem> items,
                                                                  int selectedIndex, int initialScrollOffset,
                                                                  int toggleWidth, int actionWidth,
                                                                  Consumer<Integer> responder,
                                                                  BiConsumer<Integer, Boolean> toggleResponder,
                                                                  Consumer<Integer> actionResponder) {
        return kineticRuntime().controls().addScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder);
    }

    /** Creates and registers a smooth vertical single-selection list with one real toggle and one trailing row action at the supplied Z depth. */
    default KineticToggleActionList addHighZScrollableToggleActionList(int x, int y, int width, int height,
                                                                       List<? extends ToggleActionItem> items,
                                                                       int selectedIndex, int initialScrollOffset,
                                                                       int toggleWidth, int actionWidth,
                                                                       Consumer<Integer> responder,
                                                                       BiConsumer<Integer, Boolean> toggleResponder,
                                                                       Consumer<Integer> actionResponder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableToggleActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                toggleWidth, actionWidth, responder, toggleResponder, actionResponder, zLevel);
    }

    /** Creates and registers a smooth vertical item-backed single-selection list with one trailing row action. */
    default KineticItemActionList addScrollableItemActionList(int x, int y, int width, int height,
                                                              List<? extends ItemActionItem> items, int selectedIndex,
                                                              int initialScrollOffset, int actionWidth,
                                                              Consumer<Integer> responder,
                                                              Consumer<Integer> actionResponder) {
        return kineticRuntime().controls().addScrollableItemActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, actionWidth, responder, actionResponder);
    }

    /** Creates and registers a smooth vertical item-backed single-selection list with one trailing row action at the supplied Z depth. */
    default KineticItemActionList addHighZScrollableItemActionList(int x, int y, int width, int height,
                                                                   List<? extends ItemActionItem> items,
                                                                   int selectedIndex, int initialScrollOffset,
                                                                   int actionWidth, Consumer<Integer> responder,
                                                                   Consumer<Integer> actionResponder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableItemActionList(
                x, y, width, height, items, selectedIndex, initialScrollOffset,
                actionWidth, responder, actionResponder, zLevel);
    }

    /** Creates and registers a smooth vertical single-selection list with any number of real toggles per row. */
    default KineticMultiToggleList addScrollableMultiToggleList(int x, int y, int width, int height,
                                                                List<? extends MultiToggleItem> items,
                                                                int selectedIndex, int initialScrollOffset,
                                                                Consumer<Integer> responder,
                                                                BiConsumer<ToggleHit, Boolean> toggleResponder) {
        return kineticRuntime().controls().addScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, toggleResponder);
    }

    /** Creates and registers a smooth vertical single-selection list with any number of real toggles per row at the supplied Z depth. */
    default KineticMultiToggleList addHighZScrollableMultiToggleList(int x, int y, int width, int height,
                                                                     List<? extends MultiToggleItem> items,
                                                                     int selectedIndex, int initialScrollOffset,
                                                                     Consumer<Integer> responder,
                                                                     BiConsumer<ToggleHit, Boolean> toggleResponder,
                                                                     int zLevel) {
        return kineticRuntime().controls().addHighZScrollableMultiToggleList(
                x, y, width, height, items, selectedIndex, initialScrollOffset, responder, toggleResponder, zLevel);
    }

    /** Creates and registers a smooth vertical multi-toggle list using standard Kinetic row controls. */
    default KineticToggleList addScrollableToggleList(int x, int y, int width, int height,
                                                      List<? extends ToggleItem> items, int initialScrollOffset,
                                                      BiConsumer<Integer, Boolean> responder) {
        return kineticRuntime().controls().addScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder);
    }

    /** Creates and registers a smooth vertical multi-toggle list rendered at the supplied Z depth. */
    default KineticToggleList addHighZScrollableToggleList(int x, int y, int width, int height,
                                                           List<? extends ToggleItem> items, int initialScrollOffset,
                                                           BiConsumer<Integer, Boolean> responder, int zLevel) {
        return kineticRuntime().controls().addHighZScrollableToggleList(
                x, y, width, height, items, initialScrollOffset, responder, zLevel);
    }

    /** Creates and registers a compact variable-width tab strip with API-managed horizontal scrolling. */
    default KineticTabStrip addScrollableTabStrip(int x, int y, int width, List<? extends TabStripItem> tabs,
                                                  int pinnedLeadingTabs, int selectedIndex, int initialScrollOffset,
                                                  Component previousText, Component nextText,
                                                  Consumer<Integer> responder) {
        return kineticRuntime().controls().addScrollableTabStrip(
                x, y, width, tabs, pinnedLeadingTabs, selectedIndex, initialScrollOffset,
                previousText, nextText, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default TabBarButtons addTabBar(int x, int y, int totalWidth, List<? extends Component> labels,
                                    List<? extends Component> tooltips, int selectedIndex, Consumer<Integer> responder) {
        return kineticRuntime().controls().addTabBar(x, y, totalWidth, labels, tooltips, selectedIndex, responder);
    }

    /** Creates and registers a tab bar rendered at an elevated Z depth. */
    default TabBarButtons addHighZTabBar(int x, int y, int totalWidth, List<? extends Component> labels,
                                         List<? extends Component> tooltips, int selectedIndex,
                                         Consumer<Integer> responder, int zLevel) {
        return kineticRuntime().controls().addHighZTabBar(
                x, y, totalWidth, labels, tooltips, selectedIndex, responder, zLevel);
    }

    /** Creates and registers a vertical tab bar rendered at an elevated Z depth. */
    default TabBarButtons addVerticalHighZTabBar(int x, int y, int width, List<? extends Component> labels,
                                                 List<? extends Component> tooltips, int selectedIndex,
                                                 Consumer<Integer> responder, int zLevel) {
        return kineticRuntime().controls().addVerticalHighZTabBar(
                x, y, width, labels, tooltips, selectedIndex, responder, zLevel);
    }

    /** 仅注册列表的输入事件；调用方负责通过对应 Screen 的列表渲染 API 绘制。 */
    default <T extends SmoothSelectionList<?>> T addSmoothSelectionList(T list) {
        return kineticRuntime().controls().addSmoothSelectionList(list);
    }

    /** Renders one Kinetic smooth selection list using this host's page transform. */
    default void renderSmoothSelectionList(SmoothSelectionList<?> list, GuiGraphics graphics,
                                           int mouseX, int mouseY, float partialTick) {
        kineticRuntime().renderSmoothSelectionList(list, graphics, mouseX, mouseY, partialTick);
    }

    // ---- buttons and value controls -----------------------------------------------------------------------------

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default StateButton addButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        return kineticRuntime().controls().addButton(x, y, width, text, tooltip, action);
    }

    /** Creates and registers a content-rich card button using the API-defined card height. */
    default StateButton addCardButton(int x, int y, int width, Component narration, Component tooltip, Runnable action) {
        return kineticRuntime().controls().addCardButton(x, y, width, narration, tooltip, action);
    }

    /** Creates and registers a standard item-backed button with API-managed height, rendering, and tooltip. */
    default ItemButton addItemButton(int x, int y, int width, ItemStack icon, Component text, Component tooltip,
                                     Runnable action) {
        return kineticRuntime().controls().addItemButton(x, y, width, icon, text, tooltip, action);
    }

    /** Creates and registers a managed state button whose callback needs that button instance. */
    default StateButton addButtonWithHandler(int x, int y, int width, Component text, Component tooltip,
                                             Consumer<StateButton> action) {
        return kineticRuntime().controls().addButtonWithHandler(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default StateButton addCompactButton(int x, int y, int width, Component text, Component tooltip, Runnable action) {
        return kineticRuntime().controls().addCompactButton(x, y, width, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default HighZButton addHighZButton(int x, int y, int width, Component text, Component tooltip, int zLevel,
                                       Runnable action) {
        return kineticRuntime().controls().addHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default HighZButton addCompactHighZButton(int x, int y, int width, Component text, Component tooltip, int zLevel,
                                              Runnable action) {
        return kineticRuntime().controls().addCompactHighZButton(x, y, width, text, tooltip, zLevel, action);
    }

    /** Creates and registers a standard Kinetic slider with caller-defined business rules. */
    default Slider addSlider(int x, int y, int width, Component message, double minValue, double maxValue,
                             double step, double value, Predicate<Double> validator, DoubleConsumer responder,
                             Component tooltip) {
        return kineticRuntime().controls().addSlider(
                x, y, width, message, minValue, maxValue, step, value, validator, responder, tooltip);
    }

    /** Creates and registers a toggle control without a custom validator. */
    default ToggleButton addToggleButton(int x, int y, int width, boolean value, Component onText, Component offText,
                                         Component tooltip, Consumer<Boolean> responder) {
        return addToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default ToggleButton addToggleButton(int x, int y, int width, boolean value, Component onText, Component offText,
                                         Component tooltip, Predicate<Boolean> validator, Consumer<Boolean> responder) {
        return kineticRuntime().controls().addToggleButton(
                x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** Creates and registers a compact toggle control without a custom validator. */
    default ToggleButton addCompactToggleButton(int x, int y, int width, boolean value, Component onText,
                                                Component offText, Component tooltip, Consumer<Boolean> responder) {
        return addCompactToggleButton(x, y, width, value, onText, offText, tooltip, null, responder);
    }

    /** Adds a compact themed toggle button using the standard Kinetic control height. */
    default ToggleButton addCompactToggleButton(int x, int y, int width, boolean value, Component onText,
                                                Component offText, Component tooltip, Predicate<Boolean> validator,
                                                Consumer<Boolean> responder) {
        return kineticRuntime().controls().addCompactToggleButton(
                x, y, width, value, onText, offText, tooltip, validator, responder);
    }

    /** Creates and registers a standard Kinetic multi-state cycle control. */
    default CycleButton addCycleButton(int x, int y, int width, int index, List<Component> options, Component tooltip,
                                       Predicate<Integer> validator, Consumer<Integer> responder) {
        return kineticRuntime().controls().addCycleButton(x, y, width, index, options, tooltip, validator, responder);
    }

    /** 创建并注册高层 Kinetic Toggle，统一处理模态层级、状态和 Tooltip。 */
    default HighZToggleButton addHighZToggleButton(int x, int y, int width, boolean value, Component onText,
                                                   Component offText, Component tooltip, Predicate<Boolean> validator,
                                                   Consumer<Boolean> responder, int zLevel) {
        return kineticRuntime().controls().addHighZToggleButton(
                x, y, width, value, onText, offText, tooltip, validator, responder, zLevel);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default ColorSwatchButton addColorSwatchButton(int x, int y, int rgb, Component tooltip, Runnable action) {
        return kineticRuntime().controls().addColorSwatchButton(x, y, rgb, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default ColorPreviewButton addColorPreviewButton(int x, int y, int width, int color, Component text,
                                                     Component tooltip, Runnable action) {
        return kineticRuntime().controls().addColorPreviewButton(x, y, width, color, text, tooltip, action);
    }

    /** 创建并注册标准 Kinetic 控件，统一处理 Screen Tooltip；Screen 子类优先使用此方法。 */
    default Dropdown addDropdown(int x, int y, int width, List<? extends Option> options, String selectedValue,
                                 Component tooltip, Predicate<String> validator, Consumer<String> responder) {
        return kineticRuntime().controls().addDropdown(
                x, y, width, options, selectedValue, tooltip, validator, responder);
    }

    // ---- registration and tooltips ------------------------------------------------------------------------------

    /** Registers an API-created control for rendering, input, and a single screen-managed tooltip. */
    default <T extends InternalControl> T addControl(T control, Component tooltip) {
        if (control == null) return null;
        kineticRuntime().controls().registerWidget(KineticControlBridge.widget(control), tooltip);
        return control;
    }

    /** Registers one business-specific or vanilla-special external widget that has no standard Kinetic control equivalent. */
    default <T extends AbstractWidget> T addExternalWidget(T widget, Component tooltip) {
        return kineticRuntime().controls().registerWidget(widget, tooltip);
    }

    /** Removes one widget previously registered through {@link #addExternalWidget(AbstractWidget, Component)}. */
    default void removeExternalWidget(AbstractWidget widget) {
        kineticRuntime().focus().blurControl(widget);
        kineticRuntime().controls().unregisterWidget(widget);
    }

    /** Removes one standard Kinetic control without exposing Minecraft widget types to addons. */
    default void removeKineticControl(InternalControl control) {
        if (control == null) return;
        AbstractWidget widget = KineticControlBridge.widget(control);
        blurControl(control);
        kineticRuntime().controls().unregisterWidget(widget);
    }

    /** Registers Screen Overlay Tooltip state for one standard Kinetic control. */
    default <T extends InternalControl> T registerWidgetTooltip(T control, Component tooltip) {
        if (control == null) return null;
        kineticRuntime().controls().registerWidgetTooltip(KineticControlBridge.widget(control), tooltip);
        return control;
    }

    /** Registers a dynamic Screen Overlay Tooltip evaluated at hover time for one standard Kinetic control. */
    default <T extends InternalControl> T registerDynamicWidgetTooltip(T control, Supplier<Component> tooltipSupplier) {
        if (control == null) return null;
        kineticRuntime().controls().registerDynamicWidgetTooltip(KineticControlBridge.widget(control), tooltipSupplier);
        return control;
    }

    /**
     * 请求本帧的统一 Tooltip；在渲染阶段调用，无须自行创建原版 Tooltip。
     * Requests this frame's standard one-line tooltip without restoring a same-name overload.
     */
    default void showTooltipLine(Component component) {
        kineticRuntime().controls().showTooltip(List.of(component == null ? Component.empty() : component), null);
    }

    /** Requests this frame's standard tooltip. Null maxWidth keeps text unwrapped. */
    default void showTooltip(List<? extends Component> lines, Integer maxWidth) {
        kineticRuntime().controls().showTooltip(lines, maxWidth);
    }

    /** Shows formatted tooltip lines through the screen overlay layer. */
    default void showFormattedTooltip(List<FormattedCharSequence> lines) {
        kineticRuntime().controls().showFormattedTooltip(lines);
    }

    /** 请求本帧的物品 Tooltip，由统一 Overlay 渲染。 */
    default void showItemTooltip(ItemStack stack) {
        kineticRuntime().controls().showItemTooltip(stack);
    }

    // ---- overlays -----------------------------------------------------------------------------------------------

    /** Returns whether a menu or modal dialog currently blocks this screen's underlying input and hover content. */
    default boolean overlayBlocksInput() {
        return kineticRuntime().overlays().blocksInput();
    }

    /** Closes the active standard context menu, if one is open. */
    default void closeContextMenu() {
        kineticRuntime().controls().closeContextMenu();
    }

    /** 在当前 Screen 的 UI 坐标处打开统一菜单；Screen 负责坐标转换。 */
    default void openContextMenu(double virtualX, double virtualY, List<KineticOverlays.MenuItem> items) {
        KineticCanvasTransform canvas = kineticRuntime().canvas();
        kineticRuntime().overlays().openMenu(canvas.menuX(virtualX), canvas.menuY(virtualY), items);
    }

    /** Opens the standard menu at a fixed logical width; overflowing labels scroll within their rows. */
    default void openContextMenu(double virtualX, double virtualY, List<KineticOverlays.MenuItem> items, int virtualWidth) {
        KineticCanvasTransform canvas = kineticRuntime().canvas();
        kineticRuntime().overlays().openMenu(
                canvas.menuX(virtualX), canvas.menuY(virtualY), items, canvas.menuWidth(virtualWidth));
    }

    /** 打开统一模态确认框；保存或回滚动作由 onConfirm/onCancel 回调决定。 */
    default void openDialog(Component title, Component message, Component confirmText, Component cancelText,
                            Runnable onConfirm, Runnable onCancel) {
        kineticRuntime().controls().openDialog(title, message, confirmText, cancelText, onConfirm, onCancel);
    }

    // ---- focus --------------------------------------------------------------------------------------------------

    /** 转移焦点并清除旧控件的焦点状态；传 null 等同 clearControlFocus。 */
    default void focusControl(InternalControl control) {
        kineticRuntime().focus().focusControl(control == null ? null : KineticControlBridge.widget(control));
    }

    /** 清除指定 Kinetic 控件的焦点；仅当它是当前焦点时解除 Screen 焦点。 */
    default void blurControl(InternalControl control) {
        if (control == null) return;
        kineticRuntime().focus().blurControl(KineticControlBridge.widget(control));
    }

    /** 同时清除 Screen 当前焦点和该控件的焦点状态。 */
    default void clearControlFocus() {
        kineticRuntime().focus().clearControlFocus();
    }

    /** 判断 Screen 当前焦点和 Kinetic 控件自身焦点是否一致。 */
    default boolean isControlFocused(InternalControl control) {
        return control != null && kineticRuntime().focus().isControlFocused(KineticControlBridge.widget(control));
    }

    /** Returns the currently focused standard Kinetic control, or {@code null} when focus belongs elsewhere. */
    default InternalControl focusedControl() {
        return screen().getFocused() instanceof InternalControl control ? control : null;
    }

    // ---- drafts and navigation ----------------------------------------------------------------------------------

    /** 在打开前预留独立草稿边界，避免继承父界面的草稿会话。 */
    default void reserveStandaloneDraft() {
        GuiSessionRuntime.reserveStandaloneOwner(screen());
    }

    /** 设置草稿快照与恢复函数。离开共享草稿会话时回滚；capture 应返回独立且可按 equals 比较的快照。 */
    default <T> void configureDraft(Supplier<T> capture, Consumer<T> restore) {
        GuiSessionRuntime.configureDraft(screen(), capture, restore, false);
    }

    /** 建立独立草稿保存边界，不继承父界面草稿；离开该边界时回滚未提交修改。 */
    default <T> void configureStandaloneDraft(Supplier<T> capture, Consumer<T> restore) {
        GuiSessionRuntime.configureDraft(screen(), capture, restore, true);
    }

    /** 仅草稿所有者可更新已保存基线；先完成业务持久化，再调用本方法。本方法不写入配置。 */
    default void commitDraft() {
        GuiSessionRuntime.commitDraft(screen());
    }

    /** 恢复最近保存的草稿基线；不负责关闭界面。 */
    default void discardDraft() {
        GuiSessionRuntime.discardDraft(screen());
    }

    /** 比较当前快照与保存基线，判断是否有未提交修改。 */
    default boolean hasUnsavedEdits() {
        return GuiSessionRuntime.hasUnsavedEdits(screen());
    }

    /** Explicitly declares the parent used by standard Kinetic back navigation. */
    default void setParentScreen(Screen parent) {
        GuiSessionRuntime.setExplicitParent(screen(), parent);
    }

    /** 返回父界面；离开共享草稿会话时回滚未提交修改，容器界面同时关闭容器。 */
    default void navigateBack() {
        GuiSessionRuntime.back(screen());
    }
}
