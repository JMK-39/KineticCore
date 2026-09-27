package dev.xyat.kineticcore.api.client.gui.ui;

import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * 所有控件构建器的公共选项。调用 {@link #build()} 创建控件并注册到页面。
 * Options shared by every control builder. {@link #build()} creates the control and registers it with the page.
 */
public abstract class ControlBuilder<B extends ControlBuilder<B, C>, C> {
    /** 左边界 / Left edge. */
    protected final int x;
    /** 上边界 / Top edge. */
    protected final int y;
    /** 宽度 / Width. */
    protected final int width;
    /** 静态提示 / Static tooltip. */
    protected Component tooltip;
    /** 动态提示 / Dynamic tooltip. */
    protected Supplier<Component> dynamicTooltip;
    /** 初始可用 / Initially enabled. */
    protected boolean enabled = true;
    /** 初始可见 / Initially visible. */
    protected boolean visible = true;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ControlBuilder(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /** 返回自身 / Returns this builder. */
    protected abstract B self();

    /** 静态提示 / Static tooltip. */
    public final B tooltip(Component tooltip) {
        this.tooltip = tooltip;
        this.dynamicTooltip = null;
        return self();
    }

    /** 悬停时求值的动态提示 / Tooltip evaluated on hover. */
    public final B tooltip(Supplier<Component> tooltip) {
        this.dynamicTooltip = tooltip;
        this.tooltip = null;
        return self();
    }

    /** 初始可用状态 / Initial enabled state. */
    public final B enabled(boolean enabled) {
        this.enabled = enabled;
        return self();
    }

    /** 初始可见状态 / Initial visibility. */
    public final B visible(boolean visible) {
        this.visible = visible;
        return self();
    }

    /** 创建并注册控件 / Creates and registers the control. */
    public abstract C build();
}
