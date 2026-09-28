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
    // 名称刻意避开原版控件方法（getX/getWidth/isFocused 等）：ForgeGradle 打包时会把与原版同名同签名的方法
    // 重映射为 m_*，附属中的调用会在运行时 NoSuchMethodError。
    // Names deliberately avoid vanilla widget methods (getX/getWidth/isFocused, ...): ForgeGradle remaps
    // same-name/same-descriptor methods to m_* in the production JAR, which breaks addon call sites at runtime.

    /** 左边界（页面坐标）/ Left edge in page coordinates. */
    int controlX();

    /** 上边界（页面坐标）/ Top edge in page coordinates. */
    int controlY();

    /** 宽度 / Width. */
    int controlWidth();

    /** 高度 / Height. */
    int controlHeight();

    /** 水平移动 / Moves horizontally. */
    void moveControlX(int x);

    /** 垂直移动 / Moves vertically. */
    void moveControlY(int y);

    /** 修改宽度 / Changes the width. */
    void resizeControlWidth(int width);

    /** 同时移动到指定位置 / Moves to the given position. */
    default void moveTo(int x, int y) {
        moveControlX(x);
        moveControlY(y);
    }

    /** 是否可见 / Whether the control is visible. */
    boolean controlVisible();

    /** 显示或隐藏 / Shows or hides the control. */
    void setControlVisible(boolean visible);

    /** 是否可交互 / Whether the control accepts input. */
    boolean isEnabled();

    /** 启用或禁用 / Enables or disables input. */
    void setEnabled(boolean enabled);

    /** 同时设置可见与可用 / Sets visible and enabled together. */
    default void setActive(boolean active) {
        setControlVisible(active);
        setEnabled(active);
    }

    /** 设置静态提示，null 或空白清除 / Sets a static tooltip; null or blank clears it. */
    void setTooltip(Component tooltip);

    /** 设置悬停时才求值的动态提示，null 清除 / Sets a tooltip evaluated on hover; null clears it. */
    void setTooltip(Supplier<Component> tooltip);

    /** 点是否在控件矩形内（不考虑可见性）/ Whether the point lies inside the bounds, ignoring visibility. */
    default boolean contains(double x, double y) {
        return x >= controlX() && x < controlX() + controlWidth() && y >= controlY() && y < controlY() + controlHeight();
    }

    /** 指针当前是否悬停在控件上 / Whether the pointer currently hovers this control. */
    boolean controlHovered();

    /** 控件当前是否拥有键盘焦点 / Whether the control currently owns keyboard focus. */
    boolean controlFocused();
}
