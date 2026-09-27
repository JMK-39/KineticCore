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

/** 数字自动补全输入框 / Numeric autocomplete field. */
public abstract class NumberAutoCompleteBuilder extends ControlBuilder<NumberAutoCompleteBuilder, KineticNumberAutoCompleteField> {
    /** 数字类型 / Number type. */
    protected final NumberType type;
    /** 候选字典 / Suggestion dictionary. */
    protected final Supplier<List<KineticSuggestion>> dictionary;
    /** 无障碍标签 / Narration label. */
    protected Component label = Component.empty();
    /** 是否允许负数 / Whether negatives are allowed. */
    protected boolean allowNegative = true;
    /** 闭区间范围，null 表示不限 / Inclusive range; null means unbounded. */
    protected Number min = null;
    /** 参见对应设置方法 / See the matching setter. */
    protected Number max = null;
    /** 业务校验器 / Business validator. */
    protected Predicate<Number> validator = null;
    /** 初始值 / Initial value. */
    protected String value = null;
    /** 文本变更回调 / Change callback. */
    protected Consumer<String> onChange = null;
    /** 选中候选回调 / Suggestion-picked callback. */
    protected Consumer<String> onSelect = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected NumberAutoCompleteBuilder(int x, int y, int width, NumberType type, Supplier<List<KineticSuggestion>> dictionary) {
        super(x, y, width);
        this.type = type;
        this.dictionary = dictionary;
    }

    @Override
    protected final NumberAutoCompleteBuilder self() {
        return this;
    }

    /** 无障碍标签 / Narration label. */
    public final NumberAutoCompleteBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 是否允许负数 / Whether negatives are allowed. */
    public final NumberAutoCompleteBuilder allowNegative(boolean allowNegative) {
        this.allowNegative = allowNegative;
        return this;
    }

    /** 闭区间范围，null 表示不限 / Inclusive range; null means unbounded. */
    public final NumberAutoCompleteBuilder range(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** 业务校验器 / Business validator. */
    public final NumberAutoCompleteBuilder validator(Predicate<Number> validator) {
        this.validator = validator;
        return this;
    }

    /** 初始值 / Initial value. */
    public final NumberAutoCompleteBuilder value(Number value) {
        this.value = value == null ? null : type.format(value);
        return this;
    }

    /** 文本变更回调 / Change callback. */
    public final NumberAutoCompleteBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }

    /** 选中候选回调 / Suggestion-picked callback. */
    public final NumberAutoCompleteBuilder onSelect(Consumer<String> onSelect) {
        this.onSelect = onSelect;
        return this;
    }
}
