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
}
