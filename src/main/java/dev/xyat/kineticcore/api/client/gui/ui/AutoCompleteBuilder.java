package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 自动补全输入框 / Autocomplete field. */
public abstract class AutoCompleteBuilder extends ControlBuilder<AutoCompleteBuilder, KineticAutoCompleteField> {
    /** 候选字典 / Suggestion dictionary. */
    protected final Supplier<List<KineticSuggestion>> dictionary;
    /** 无障碍标签 / Narration label. */
    protected Component label = Component.empty();
    /** 占位提示 / Placeholder. */
    protected Component placeholder = null;
    /** 初始文本 / Initial text. */
    protected String value = null;
    /** 最大长度 / Maximum length. */
    protected int maxLength = -1;
    /** 文本变更回调 / Change callback. */
    protected Consumer<String> onChange = null;
    /** 选中候选回调 / Suggestion-picked callback. */
    protected Consumer<String> onSelect = null;

    /** 默认值（显示黑色，其它内容绿色）/ Default value (drawn black; other content green). */
    protected String defaultText = null;
    /** 自定义文字颜色规则 / Custom value color rule. */
    protected java.util.function.Function<String, Integer> valueColor = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected AutoCompleteBuilder(int x, int y, int width, Supplier<List<KineticSuggestion>> dictionary) {
        super(x, y, width);
        this.dictionary = dictionary;
    }

    @Override
    protected final AutoCompleteBuilder self() {
        return this;
    }

    /** 无障碍标签 / Narration label. */
    public final AutoCompleteBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 占位提示 / Placeholder. */
    public final AutoCompleteBuilder placeholder(Component placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /** 初始文本 / Initial text. */
    public final AutoCompleteBuilder value(String value) {
        this.value = value;
        return this;
    }

    /** 最大长度 / Maximum length. */
    public final AutoCompleteBuilder maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /** 文本变更回调 / Change callback. */
    public final AutoCompleteBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    /** 选中候选回调 / Suggestion-picked callback. */
    public final AutoCompleteBuilder onSelect(Consumer<String> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /**
     * 默认值：内容等于它时显示黑色，其它非空内容（已修改）显示绿色。
     * Default value: text equal to it is drawn black, any other non-empty text (modified) green.
     */
    public final AutoCompleteBuilder defaultText(String defaultText) {
        this.defaultText = defaultText;
        return this;
    }

    /**
     * 按内容自定义文字颜色（如货币不足红色），返回 null 使用标准规则。
     * Custom text color per value (for example red when unaffordable); return null for the standard rule.
     */
    public final AutoCompleteBuilder valueColor(java.util.function.Function<String, Integer> valueColor) {
        this.valueColor = valueColor;
        return this;
    }
}
