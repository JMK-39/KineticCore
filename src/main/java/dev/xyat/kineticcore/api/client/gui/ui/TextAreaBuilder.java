package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticTextArea;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** 多行文本框 / Text area. */
public abstract class TextAreaBuilder extends ControlBuilder<TextAreaBuilder, KineticTextArea> {
    /** 高度 / Height. */
    protected final int height;
    /** 无障碍标签 / Narration label. */
    protected Component label = Component.empty();
    /** 占位提示 / Placeholder. */
    protected Component placeholder = null;
    /** 初始文本 / Initial text. */
    protected String value = null;
    /** 最大字符数 / Character limit. */
    protected int maxLength = -1;
    /** 文本变更回调 / Change callback. */
    protected Consumer<String> onChange = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected TextAreaBuilder(int x, int y, int width, int height) {
        super(x, y, width);
        this.height = height;
    }

    @Override
    protected final TextAreaBuilder self() {
        return this;
    }

    /** 无障碍标签 / Narration label. */
    public final TextAreaBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 占位提示 / Placeholder. */
    public final TextAreaBuilder placeholder(Component placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /** 初始文本 / Initial text. */
    public final TextAreaBuilder value(String value) {
        this.value = value;
        return this;
    }

    /** 最大字符数 / Character limit. */
    public final TextAreaBuilder maxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /** 文本变更回调 / Change callback. */
    public final TextAreaBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }
}
