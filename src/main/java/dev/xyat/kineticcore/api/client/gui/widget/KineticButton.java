package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

/**
 * 标准 Kinetic 按钮：文字、选中态与错误态由 API 统一绘制。
 * Standard Kinetic button; label, selected and error states are drawn by the API theme.
 */
public interface KineticButton extends KineticControl {
    /** 当前文字 / Current label. */
    Component text();

    /** 修改文字 / Changes the label. */
    void setText(Component text);

    /** 是否处于选中态 / Whether the button is shown as selected. */
    boolean isSelected();

    /** 设置选中态 / Sets the selected state. */
    void setSelected(boolean selected);

    /** 是否处于错误态 / Whether the button is shown in error state. */
    boolean isError();

    /** 设置错误态 / Sets the error state. */
    void setError(boolean error);
}
