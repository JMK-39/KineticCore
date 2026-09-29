package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticCustomControl;
import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiToggleItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleItem;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * 页面的控件工厂。每个方法返回一个构建器，调用 {@code build()} 后控件立即注册到页面，渲染、输入、焦点与提示均由页面负责。
 * 坐标为页面坐标；标准控件高度由 API 决定。
 * The page's control factory. Every method returns a builder; {@code build()} registers the control with the page,
 * which then owns rendering, input, focus and tooltips. Coordinates are page coordinates; standard control
 * heights are decided by the API.
 */
public interface KineticUi {
    /**
     * 标准按钮 / Standard button.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @return a builder; call {@code build()} to create and register the control
     */
    ButtonBuilder button(int x, int y, int width);

    /**
     * 物品卡片按钮 / Item card button showing an item icon above its text.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param icon item drawn on the card
     * @return a builder; call {@code build()} to create and register the control
     */
    ItemButtonBuilder itemButton(int x, int y, int width, ItemStack icon);

    /**
     * 开关按钮 / On/off toggle button.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @return a builder; call {@code build()} to create and register the control
     */
    ToggleBuilder toggle(int x, int y, int width);

    /**
     * 循环按钮 / Button that steps through a fixed list of labels on each click.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param options labels in cycle order
     * @return a builder; call {@code build()} to create and register the control
     */
    CycleButtonBuilder cycleButton(int x, int y, int width, List<Component> options);

    /**
     * 颜色预览按钮 / Button that previews a color and usually opens the color picker.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param rgb initial color as {@code 0xRRGGBB}
     * @return a builder; call {@code build()} to create and register the control
     */
    ColorButtonBuilder colorButton(int x, int y, int width, int rgb);

    /**
     * 颜色色块 / Small square color swatch with a standard size.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param rgb initial color as {@code 0xRRGGBB}
     * @return a builder; call {@code build()} to create and register the control
     */
    ColorSwatchBuilder colorSwatch(int x, int y, int rgb);

    /**
     * 滑块 / Horizontal value slider.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @return a builder; call {@code build()} to create and register the control
     */
    SliderBuilder slider(int x, int y, int width);

    /**
     * 单行文本框 / Single-line text input.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @return a builder; call {@code build()} to create and register the control
     */
    TextFieldBuilder textField(int x, int y, int width);

    /**
     * 多行文本框 / Multi-line text input with its own scrolling.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width area width in page pixels
     * @param height area height in page pixels
     * @return a builder; call {@code build()} to create and register the control
     */
    TextAreaBuilder textArea(int x, int y, int width, int height);

    /**
     * 数字输入框 / Numeric input that only accepts text of the given number type; range and sign rules are set on the
     * builder.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param type integer, long or decimal syntax
     * @return a builder; call {@code build()} to create and register the control
     */
    NumberFieldBuilder numberField(int x, int y, int width, NumberType type);

    /**
     * 自动补全输入框 / Text input with a suggestion popup.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param dictionary queried whenever the suggestions refresh; values are inserted, translations are
     *   display-only
     * @return a builder; call {@code build()} to create and register the control
     */
    AutoCompleteBuilder autoComplete(int x, int y, int width, Supplier<List<KineticSuggestion>> dictionary);

    /**
     * 数字自动补全输入框 / Numeric input with a suggestion popup.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param type integer, long or decimal syntax
     * @param dictionary queried whenever the suggestions refresh
     * @return a builder; call {@code build()} to create and register the control
     */
    NumberAutoCompleteBuilder numberAutoComplete(int x, int y, int width, NumberType type,
                                                 Supplier<List<KineticSuggestion>> dictionary);

    /**
     * 下拉选择 / Dropdown that picks one option from a list.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param options options in display order
     * @return a builder; call {@code build()} to create and register the control
     */
    DropdownBuilder dropdown(int x, int y, int width, List<KineticDropdown.Option> options);

    /**
     * 等宽标签栏（width 为总宽，竖排时为单个宽度）/ Tab bar with equal-width tabs.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width total width, or the width of one tab when the bar is vertical
     * @param labels tab labels in order
     * @return a builder; call {@code build()} to create and register the control
     */
    TabBarBuilder tabBar(int x, int y, int width, List<Component> labels);

    /**
     * 可滚动变宽标签条 / Horizontally scrolling strip of tabs sized to their labels.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width control width in page pixels; the height is the API's standard control height
     * @param tabs tabs in order
     * @return a builder; call {@code build()} to create and register the control
     */
    TabStripBuilder tabStrip(int x, int y, int width, List<TabStripItem> tabs);

    /**
     * 单选列表 / Scrolling single-selection list.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    SelectionListBuilder selectionList(int x, int y, int width, int height, List<SelectionItem> items);

    /**
     * 物品单选列表 / Scrolling single-selection list with an item slot in each row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    ItemSelectionListBuilder itemSelectionList(int x, int y, int width, int height, List<ItemSelectionItem> items);

    /**
     * 物品网格 / Scrolling grid of item slots.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param density slot size and spacing preset
     * @param items initial slots
     * @return a builder; call {@code build()} to create and register the control
     */
    ItemGridBuilder itemGrid(int x, int y, int width, int height, ItemGridDensity density, List<ItemGridItem> items);

    /**
     * 带行尾按钮的列表 / Scrolling single-selection list with one trailing button per row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    ActionListBuilder actionList(int x, int y, int width, int height, List<ActionItem> items);

    /**
     * 带行尾按钮的物品列表 / Item list with one trailing button per row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    ItemActionListBuilder itemActionList(int x, int y, int width, int height, List<ItemActionItem> items);

    /**
     * 多按钮列表 / Scrolling list with several trailing buttons per row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    MultiActionListBuilder multiActionList(int x, int y, int width, int height, List<MultiActionItem> items);

    /**
     * 开关与按钮列表 / Scrolling list with one toggle and one trailing button per row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    ToggleActionListBuilder toggleActionList(int x, int y, int width, int height, List<ToggleActionItem> items);

    /**
     * 多开关列表 / Scrolling list with several toggles per row.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    MultiToggleListBuilder multiToggleList(int x, int y, int width, int height, List<MultiToggleItem> items);

    /**
     * 开关列表 / Scrolling list of rows that are each one toggle.
     *
     * @param x left edge in page coordinates
     * @param y top edge in page coordinates
     * @param width viewport width in page pixels
     * @param height viewport height in page pixels
     * @param items initial rows
     * @return a builder; call {@code build()} to create and register the control
     */
    ToggleListBuilder toggleList(int x, int y, int width, int height, List<ToggleItem> items);

    /**
     * 注册自定义控件 / Registers a custom control so the page renders it and routes input, focus and tooltips to it.
     *
     * @param control control to register
     * @param <C> control type
     * @return {@code control}, for chaining
     */
    <C extends KineticCustomControl> C add(C control);

    /** 移除控件 / Removes a control from the page, including its focus and tooltip; unknown controls are ignored. */
    void remove(KineticControl control);

    /**
     * 返回一个绑定到滚动视口的构建器：通过它创建的控件被裁剪到视口内，并随 pixelOffset 垂直偏移。仅画布页面支持。
     * Returns a builder bound to a scrolling viewport: controls created through it are clipped to the viewport and
     * shifted vertically by pixelOffset. Canvas pages only.
     */
    KineticUi scrollViewport(int left, int top, int right, int bottom, DoubleSupplier pixelOffset);
}
