package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticDropdown;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** 下拉选择 / Dropdown. */
public abstract class DropdownBuilder extends ControlBuilder<DropdownBuilder, KineticDropdown> {
    /** 选项 / Options. */
    protected final List<KineticDropdown.Option> options;
    /** 初始选中值 / Initially selected value. */
    protected String selected = null;
    /** 校验器 / Validator. */
    protected Predicate<String> validator = null;
    /** 选择变更回调 / Change callback. */
    protected Consumer<String> onChange = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected DropdownBuilder(int x, int y, int width, List<KineticDropdown.Option> options) {
        super(x, y, width);
        this.options = options;
    }

    @Override
    protected final DropdownBuilder self() {
        return this;
    }

    /** 初始选中值 / Initially selected value. */
    public final DropdownBuilder selected(String value) {
        this.selected = value;
        return this;
    }

    /** 校验器 / Validator. */
    public final DropdownBuilder validator(Predicate<String> validator) {
        this.validator = validator;
        return this;
    }

    /** 选择变更回调 / Change callback. */
    public final DropdownBuilder onChange(Consumer<String> onChange) {
        this.onChange = onChange;
        return this;
    }
}
