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

/** 单行文本框 / Text field. */
public abstract class TextFieldBuilder extends ControlBuilder<TextFieldBuilder, KineticTextField> {
    /** 无障碍标签 / Narration label. */
    protected Component label = Component.empty();
    /** 占位提示 / Placeholder. */
    protected Component placeholder = null;
    /** 业务校验器（错误边框）/ Business validator (error border). */
    protected Predicate<String> validator = null;
    /** 初始文本 / Initial text. */
    protected String value = null;
    /** 最大长度 / Maximum length. */
    protected int maxLength = -1;
    /** 文本变更回调 / Change callback. */
    protected Consumer<String> onChange = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected TextFieldBuilder(int x, int y, int width) {
        super(x, y, width);

    }

    @Override
    protected final TextFieldBuilder self() {
        return this;
    }

    /** 无障碍标签 / Narration label. */
    public final TextFieldBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 占位提示 / Placeholder. */
    public final TextFieldBuilder placeholder(Component placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /** 业务校验器（错误边框）/ Business validator (error border). */
    public final TextFieldBuilder validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    /** 初始文本 / Initial text. */
    public final TextFieldBuilder value(String value) {
        this.value = value;
        return this;
    }

    /** 最大长度 / Maximum length. */
    public final TextFieldBuilder maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /** 文本变更回调 / Change callback. */
    public final TextFieldBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }
}
