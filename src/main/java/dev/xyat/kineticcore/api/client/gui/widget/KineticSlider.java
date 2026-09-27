package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

/** 数值滑块 / Numeric slider. */
public interface KineticSlider extends KineticControl {
    /** 当前数值 / Current value. */
    double value();

    /** 同步数值，不触发回调 / Synchronizes the value without invoking the change callback. */
    void setValue(double value);

    /** 当前值是否被校验器拒绝 / Whether the validator currently rejects the value. */
    boolean isError();

    /** 标签 / Label. */
    Component text();

    /** 修改标签 / Changes the label. */
    void setText(Component text);
}
