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
    /** 标准按钮 / Standard button. */
    ButtonBuilder button(int x, int y, int width);

    /** 物品卡片按钮 / Item card button. */
    ItemButtonBuilder itemButton(int x, int y, int width, ItemStack icon);

    /** 开关按钮 / Toggle button. */
    ToggleBuilder toggle(int x, int y, int width);

    /** 循环按钮 / Cycle button. */
    CycleButtonBuilder cycleButton(int x, int y, int width, List<Component> options);

    /** 颜色预览按钮 / Color preview button. */
    ColorButtonBuilder colorButton(int x, int y, int width, int rgb);

    /** 颜色色块 / Color swatch. */
    ColorSwatchBuilder colorSwatch(int x, int y, int rgb);

    /** 滑块 / Slider. */
    SliderBuilder slider(int x, int y, int width);

    /** 单行文本框 / Text field. */
    TextFieldBuilder textField(int x, int y, int width);

    /** 多行文本框 / Text area. */
    TextAreaBuilder textArea(int x, int y, int width, int height);

    /** 数字输入框 / Number field. */
    NumberFieldBuilder numberField(int x, int y, int width, NumberType type);

    /** 自动补全输入框 / Autocomplete field. */
    AutoCompleteBuilder autoComplete(int x, int y, int width, Supplier<List<KineticSuggestion>> dictionary);

    /** 数字自动补全输入框 / Numeric autocomplete field. */
    NumberAutoCompleteBuilder numberAutoComplete(int x, int y, int width, NumberType type,
                                                 Supplier<List<KineticSuggestion>> dictionary);

    /** 下拉选择 / Dropdown. */
    DropdownBuilder dropdown(int x, int y, int width, List<KineticDropdown.Option> options);

    /** 等宽标签栏（width 为总宽，竖排时为单个宽度）/ Tab bar (total width; one tab's width when vertical). */
    TabBarBuilder tabBar(int x, int y, int width, List<Component> labels);

    /** 可滚动变宽标签条 / Scrollable tab strip. */
    TabStripBuilder tabStrip(int x, int y, int width, List<TabStripItem> tabs);

    /** 单选列表 / Selection list. */
    SelectionListBuilder selectionList(int x, int y, int width, int height, List<SelectionItem> items);

    /** 物品单选列表 / Item selection list. */
    ItemSelectionListBuilder itemSelectionList(int x, int y, int width, int height, List<ItemSelectionItem> items);

    /** 物品网格 / Item grid. */
    ItemGridBuilder itemGrid(int x, int y, int width, int height, ItemGridDensity density, List<ItemGridItem> items);

    /** 带行尾按钮的列表 / Action list. */
    ActionListBuilder actionList(int x, int y, int width, int height, List<ActionItem> items);

    /** 带行尾按钮的物品列表 / Item action list. */
    ItemActionListBuilder itemActionList(int x, int y, int width, int height, List<ItemActionItem> items);

    /** 多按钮列表 / Multi-action list. */
    MultiActionListBuilder multiActionList(int x, int y, int width, int height, List<MultiActionItem> items);

    /** 开关与按钮列表 / Toggle-action list. */
    ToggleActionListBuilder toggleActionList(int x, int y, int width, int height, List<ToggleActionItem> items);

    /** 多开关列表 / Multi-toggle list. */
    MultiToggleListBuilder multiToggleList(int x, int y, int width, int height, List<MultiToggleItem> items);

    /** 开关列表 / Toggle list. */
    ToggleListBuilder toggleList(int x, int y, int width, int height, List<ToggleItem> items);

    /** 注册自定义控件 / Registers a custom control. */
    <C extends KineticCustomControl> C add(C control);

    /** 移除控件 / Removes a control. */
    void remove(KineticControl control);

    /**
     * 返回一个绑定到滚动视口的构建器：通过它创建的控件被裁剪到视口内，并随 pixelOffset 垂直偏移。仅画布页面支持。
     * Returns a builder bound to a scrolling viewport: controls created through it are clipped to the viewport and
     * shifted vertically by pixelOffset. Canvas pages only.
     */
    KineticUi scrollViewport(int left, int top, int right, int bottom, DoubleSupplier pixelOffset);
}
