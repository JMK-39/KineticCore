package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/** 下拉选择按钮 / Dropdown selection button. */
public interface KineticDropdown extends KineticButton {
    /** 一个下拉选项 / One dropdown option. */
    record Option(String value, Component translation, Component tooltip) {
        /** 规范化选项：值去空白且不能为空，显示元数据可为空 / Trims the value (must not be blank); display metadata may be null. */
        public Option {
            value = Objects.requireNonNull(value, "value").trim();
            if (value.isEmpty()) throw new IllegalArgumentException("dropdown value cannot be blank");
            translation = Objects.requireNonNullElse(translation, Component.empty());
            tooltip = Objects.requireNonNullElse(tooltip, Component.empty());
        }

        /** 无提示的选项 / Option without a tooltip. */
        public Option(String value, Component translation) {
            this(value, translation, null);
        }
    }

    /** 全部选项 / All options. */
    List<Option> options();

    /** 当前选中下标 / Selected index. */
    int selectedIndex();

    /** 当前选中值 / Selected value. */
    String selectedValue();

    /** 当前选中项 / Selected option. */
    Option selectedOption();

    /** 同步选中值，不触发回调 / Synchronizes the selection without invoking the callback. */
    void setSelectedValue(String value);
}
