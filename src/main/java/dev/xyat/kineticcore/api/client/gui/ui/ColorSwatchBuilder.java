package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticColorSwatch;

/** 颜色色块 / Color swatch. */
public abstract class ColorSwatchBuilder extends ControlBuilder<ColorSwatchBuilder, KineticColorSwatch> {
    /** 颜色 / Color. */
    protected final int rgb;
    /** 点击回调 / Click action. */
    protected Runnable onClick = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ColorSwatchBuilder(int x, int y, int width, int rgb) {
        super(x, y, width);
        this.rgb = rgb;
    }

    @Override
    protected final ColorSwatchBuilder self() {
        return this;
    }

    /** 点击回调 / Click action. */
    public final ColorSwatchBuilder onClick(Runnable action) {
        this.onClick = action;
        return this;
    }
}
