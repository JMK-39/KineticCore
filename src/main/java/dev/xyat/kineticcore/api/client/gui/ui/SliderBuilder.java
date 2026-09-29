package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticSlider;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

/** 数值滑块 / Slider. */
public abstract class SliderBuilder extends ControlBuilder<SliderBuilder, KineticSlider> {
    /** 标签 / Label. */
    protected Component label = Component.empty();
    /** 范围与步长 / Range and step. */
    protected double min = 0D;
    /** 参见对应设置方法 / See the matching setter. */
    protected double max = 1D;
    /** 参见对应设置方法 / See the matching setter. */
    protected double step = 0D;
    /** 初始值 / Initial value. */
    protected double value = 0D;
    /** 校验器 / Validator. */
    protected Predicate<Double> validator = null;
    /** 值变更回调 / Change callback. */
    protected DoubleConsumer onChange = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected SliderBuilder(int x, int y, int width) {
        super(x, y, width);

    }

    @Override
    protected final SliderBuilder self() {
        return this;
    }

    /** 标签 / Label. */
    public final SliderBuilder label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    /** 范围与步长 / Range and step. */
    public final SliderBuilder range(double min, double max, double step) {
        this.min = min;
        this.max = max;
        this.step = step;
        return this;
    }

    /** 初始值 / Initial value. */
    public final SliderBuilder value(double value) {
        this.value = value;
        return this;
    }

    /** 校验器 / Validator. */
    public final SliderBuilder validator(Predicate<Double> validator) {
        this.validator = validator;
        return this;
    }

    /** 值变更回调 / Change callback. */
    public final SliderBuilder onChange(DoubleConsumer onChange) {
        this.onChange = onChange;
        return this;
    }
}
