package dev.xyat.kineticcore.api.client.gui.ui;


/**
 * 支持层级抬升的控件构建器：layer ≥ 1 的控件绘制在普通控件与物品图标之上（弹层、侧边栏等）。
 * Builder for controls that can be raised: controls with layer ≥ 1 draw above normal controls and item icons
 * (popups, side panels).
 */
public abstract class LayeredControlBuilder<B extends LayeredControlBuilder<B, C>, C>
        extends ControlBuilder<B, C> {
    /** 层级，0 为普通 / Layer; 0 is normal. */
    protected int layer;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected LayeredControlBuilder(int x, int y, int width) {
        super(x, y, width);
    }

    /** 设置层级（≥ 0）/ Sets the layer (≥ 0). */
    public final B layer(int layer) {
        this.layer = Math.max(0, layer);
        return self();
    }
}
