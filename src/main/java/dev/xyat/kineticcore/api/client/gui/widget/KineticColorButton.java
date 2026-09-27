package dev.xyat.kineticcore.api.client.gui.widget;

/** 带颜色预览的文字按钮 / Labeled button with a color preview. */
public interface KineticColorButton extends KineticButton {
    /** 当前 RGB / Current RGB value. */
    int rgb();

    /** 修改 RGB / Changes the RGB value. */
    void setRgb(int rgb);
}
