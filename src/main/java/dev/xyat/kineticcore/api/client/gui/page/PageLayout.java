package dev.xyat.kineticcore.api.client.gui.page;

/** 页面坐标系 / Page coordinate system. */
public enum PageLayout {
    /**
     * 固定逻辑画布（默认 640×360），在任意分辨率下等比缩放并居中；绝大多数界面使用它。
     * Fixed logical canvas (640×360 by default) scaled uniformly and centered at any resolution; use it for
     * almost every page.
     */
    CANVAS,
    /**
     * 原生屏幕坐标（GUI 缩放后的像素），用于需要贴合真实屏幕的编辑器，例如 HUD 位置编辑。
     * Native screen coordinates (GUI-scaled pixels) for editors that must match the real screen, such as HUD
     * position editors.
     */
    NATIVE
}
