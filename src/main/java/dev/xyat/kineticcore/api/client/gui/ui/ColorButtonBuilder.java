package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticColorButton;
import net.minecraft.network.chat.Component;

/** 颜色预览按钮 / Color preview button. */
public abstract class ColorButtonBuilder extends ControlBuilder<ColorButtonBuilder, KineticColorButton> {
    /** 颜色 / Color. */
    protected final int rgb;
    /** 文字 / Label. */
    protected Component text = Component.empty();
    /** 点击回调 / Click action. */
    protected Runnable onClick = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ColorButtonBuilder(int x, int y, int width, int rgb) {
        super(x, y, width);
        this.rgb = rgb;
    }

    @Override
    protected final ColorButtonBuilder self() {
        return this;
    }

    /** 文字 / Label. */
    public final ColorButtonBuilder text(Component text) {
        this.text = text == null ? Component.empty() : text;
        return this;
    }

    /** 点击回调 / Click action. */
    public final ColorButtonBuilder onClick(Runnable action) {
        this.onClick = action;
        return this;
    }
}
