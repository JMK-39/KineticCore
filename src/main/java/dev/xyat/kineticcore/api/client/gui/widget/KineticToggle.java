package dev.xyat.kineticcore.api.client.gui.widget;

/** 开关按钮 / Two-state toggle button. */
public interface KineticToggle extends KineticButton {
    /** 当前值 / Current value. */
    boolean value();

    /** 同步值，不触发回调 / Synchronizes the value without invoking the change callback. */
    void setValue(boolean value);
}
