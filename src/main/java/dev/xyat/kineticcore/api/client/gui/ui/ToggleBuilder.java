package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticToggle;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** 开关按钮 / Toggle button. */
public abstract class ToggleBuilder extends LayeredControlBuilder<ToggleBuilder, KineticToggle> {
    /** 初始值 / Initial value. */
    protected boolean value = false;
    /** 开/关文字 / On and off labels. */
    protected Component onText = Component.empty();
    /** 参见对应设置方法 / See the matching setter. */
    protected Component offText = Component.empty();
    /** 校验器，拒绝则不切换 / Validator; rejected values do not toggle. */
    protected Predicate<Boolean> validator = null;
    /** 值变更回调 / Change callback. */
    protected Consumer<Boolean> onChange = null;
    /** 紧凑样式（不能与 layer 同用）/ Compact style (cannot be combined with layer). */
    protected boolean compact = false;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ToggleBuilder(int x, int y, int width) {
        super(x, y, width);

    }

    @Override
    protected final ToggleBuilder self() {
        return this;
    }

    /** 初始值 / Initial value. */
    public final ToggleBuilder value(boolean value) {
        this.value = value;
        return this;
    }

    /** 开/关文字 / On and off labels. */
    public final ToggleBuilder labels(Component onText, Component offText) {
        this.onText = onText == null ? Component.empty() : onText;
        this.offText = offText == null ? Component.empty() : offText;
        return this;
    }

    /** 校验器，拒绝则不切换 / Validator; rejected values do not toggle. */
    public final ToggleBuilder validator(Predicate<Boolean> validator) {
        this.validator = validator;
        return this;
    }

    /** 值变更回调 / Change callback. */
    public final ToggleBuilder onChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
        return this;
    }

    /** 紧凑样式（不能与 layer 同用）/ Compact style (cannot be combined with layer). */
    public final ToggleBuilder compact() {
        this.compact = true;
        return this;
    }
}
