package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticCycleButton;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** 循环按钮 / Cycle button. */
public abstract class CycleButtonBuilder extends ControlBuilder<CycleButtonBuilder, KineticCycleButton> {
    /** 选项 / Options. */
    protected final List<Component> options;
    /** 初始下标 / Initial index. */
    protected int index = 0;
    /** 校验器 / Validator. */
    protected Predicate<Integer> validator = null;
    /** 下标变更回调 / Change callback. */
    protected Consumer<Integer> onChange = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected CycleButtonBuilder(int x, int y, int width, List<Component> options) {
        super(x, y, width);
        this.options = options;
    }

    @Override
    protected final CycleButtonBuilder self() {
        return this;
    }

    /** 初始下标 / Initial index. */
    public final CycleButtonBuilder index(int index) {
        this.index = index;
        return this;
    }

    /** 校验器 / Validator. */
    public final CycleButtonBuilder validator(Predicate<Integer> validator) {
        this.validator = validator;
        return this;
    }

    /** 下标变更回调 / Change callback. */
    public final CycleButtonBuilder onChange(Consumer<Integer> onChange) {
        this.onChange = onChange;
        return this;
    }
}
