package dev.xyat.kineticcore.api.client.gui.widget;

/** 方形颜色色块按钮 / Square color swatch button. */
public interface KineticColorSwatch extends KineticControl {
    /** 当前 RGB / Current RGB value. */
    int rgb();

    /** 修改 RGB / Changes the RGB value. */
    void setRgb(int rgb);
}
