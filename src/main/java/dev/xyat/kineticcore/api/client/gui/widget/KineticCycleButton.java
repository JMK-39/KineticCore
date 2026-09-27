package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

import java.util.List;

/** 循环切换选项的按钮 / Button cycling through a fixed option list. */
public interface KineticCycleButton extends KineticButton {
    /** 当前选项下标 / Current option index. */
    int index();

    /** 同步下标，不触发回调 / Synchronizes the index without invoking the change callback. */
    void setIndex(int index);

    /** 全部选项 / All options. */
    List<Component> options();
}
