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

/** 数字输入框 / Number field. */
public abstract class NumberFieldBuilder extends ControlBuilder<NumberFieldBuilder, KineticNumberField> {
    /** 数字类型 / Number type. */
    protected final NumberType type;
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

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected NumberFieldBuilder(int x, int y, int width, NumberType type) {
        super(x, y, width);
        this.type = type;
    }

    @Override
    protected final NumberFieldBuilder self() {
        return this;
    }

    /** 无障碍标签 / Narration label. */
    public final NumberFieldBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 是否允许负数 / Whether negatives are allowed. */
    public final NumberFieldBuilder allowNegative(boolean allowNegative) {
        this.allowNegative = allowNegative;
        return this;
    }

    /** 闭区间范围，null 表示不限 / Inclusive range; null means unbounded. */
    public final NumberFieldBuilder range(Number min, Number max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** 业务校验器 / Business validator. */
    public final NumberFieldBuilder validator(Predicate<Number> validator) {
        this.validator = validator;
        return this;
    }

    /** 初始值 / Initial value. */
    public final NumberFieldBuilder value(Number value) {
        this.value = value == null ? null : type.format(value);
        return this;
    }

    /** 文本变更回调 / Change callback. */
    public final NumberFieldBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }
}
