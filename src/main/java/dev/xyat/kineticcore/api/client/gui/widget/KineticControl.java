package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * 所有 Kinetic 控件共享的几何、可见性、可用性与提示契约。控件由 {@code KineticUi} 创建并自动注册到所属页面，
 * 渲染、输入分发与提示显示都由页面负责，附属无需也无法接触原版 Widget。
 * Geometry, visibility, enabled-state and tooltip contract shared by every Kinetic control. Controls are
 * created through {@code KineticUi} and registered with their page automatically; the page owns rendering,
 * input dispatch and tooltips, so addons never touch vanilla widgets.
 */
public interface KineticControl {
    /** 左边界（页面坐标）/ Left edge in page coordinates. */
    int getX();

    /** 上边界（页面坐标）/ Top edge in page coordinates. */
    int getY();

    /** 宽度 / Width. */
    int getWidth();

    /** 高度 / Height. */
    int getHeight();

    /** 水平移动 / Moves horizontally. */
    void setX(int x);

    /** 垂直移动 / Moves vertically. */
    void setY(int y);

    /** 修改宽度 / Changes the width. */
    void setWidth(int width);

    /** 同时移动到指定位置 / Moves to the given position. */
    default void moveTo(int x, int y) {
        setX(x);
        setY(y);
    }

    /** 是否可见 / Whether the control is visible. */
    boolean isVisible();

    /** 显示或隐藏 / Shows or hides the control. */
    void setVisible(boolean visible);

    /** 是否可交互 / Whether the control accepts input. */
    boolean isEnabled();

    /** 启用或禁用 / Enables or disables input. */
    void setEnabled(boolean enabled);

    /** 同时设置可见与可用 / Sets visible and enabled together. */
    default void setActive(boolean active) {
        setVisible(active);
        setEnabled(active);
    }

    /** 设置静态提示，null 或空白清除 / Sets a static tooltip; null or blank clears it. */
    void setTooltip(Component tooltip);

    /** 设置悬停时才求值的动态提示，null 清除 / Sets a tooltip evaluated on hover; null clears it. */
    void setTooltip(Supplier<Component> tooltip);

    /** 点是否在控件矩形内（不考虑可见性）/ Whether the point lies inside the bounds, ignoring visibility. */
    default boolean contains(double x, double y) {
        return x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    /** 指针当前是否悬停在控件上 / Whether the pointer currently hovers this control. */
    boolean isHovered();

    /** 控件当前是否拥有键盘焦点 / Whether the control currently owns keyboard focus. */
    boolean isFocused();
}
