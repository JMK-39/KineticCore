package dev.xyat.kineticcore.api.client.gui.widget;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.internal.client.gui.page.PreviewAccess;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets;
import dev.xyat.kineticcore.internal.client.gui.widget.render.KineticEntityPreview.EntityPreviewRenderer;
import net.minecraft.world.entity.Entity;

/**
 * 实体预览渲染器：按实体 ID 缓存实体、自动缩放、旋转，并按状态键保存缩放。配合
 * {@code KineticPage.registerPreviewZoomArea} 支持 Ctrl+滚轮缩放。
 * Entity preview renderer: caches entities by id, auto-scales, rotates and keeps per-key zoom. Combine with
 * {@code KineticPage.registerPreviewZoomArea} for ctrl+wheel zoom.
 */
public final class KineticEntityPreview {
    /** 默认缩放百分比 / Default zoom percent. */
    public static final int DEFAULT_ZOOM_PERCENT = 100;

    private final EntityPreviewRenderer renderer;

    private KineticEntityPreview(EntityPreviewRenderer renderer) {
        this.renderer = renderer;
        PreviewAccess.register(this, renderer);
    }

    /** 使用默认参数创建 / Creates a preview with default settings. */
    public static KineticEntityPreview create() {
        return new KineticEntityPreview(KineticWidgets.createEntityPreviewRenderer());
    }

    /** 自定义缓存大小、填充比例与最大自动缩放 / Creates a preview with custom cache size, fill ratio and auto-scale cap. */
    public static KineticEntityPreview create(int maxCacheSize, float fillRatio, float maxAutoScaleFactor) {
        return new KineticEntityPreview(KineticWidgets.createEntityPreviewRenderer(maxCacheSize, fillRatio, maxAutoScaleFactor));
    }

    /** 旋转速度百分比 / Rotation speed percent. */
    public void setRotationSpeedPercent(int percent) {
        renderer.setRotationSpeedPercent(percent);
    }

    /** 旋转方向 / Rotation direction. */
    public void setClockwise(boolean clockwise) {
        renderer.setClockwise(clockwise);
    }

    /** 某状态键的缩放百分比 / Zoom percent of a state key. */
    public int zoomPercent(String stateKey) {
        return renderer.getZoomPercent(stateKey);
    }

    /** 设置缩放百分比 / Sets the zoom percent. */
    public void setZoomPercent(String stateKey, int zoomPercent) {
        renderer.setZoomPercent(stateKey, zoomPercent);
    }

    /** 按滚轮增量调整缩放 / Adjusts the zoom by a wheel delta. */
    public void adjustZoom(String stateKey, double delta) {
        renderer.adjustZoom(stateKey, delta);
    }

    /** 在框内绘制指定实体类型，返回是否成功 / Draws an entity type inside a box; returns whether it rendered. */
    public boolean render(KineticGraphics graphics, String entityId, String stateKey, int x, int y, int width, int height,
                          boolean hovered) {
        return renderer.renderCanvas(GuiGraphicsAdapter.unwrap(graphics), entityId, stateKey, x, y, width, height, hovered);
    }

    /** 在框内绘制已有实体实例 / Draws an existing entity instance inside a box. */
    public boolean render(KineticGraphics graphics, Entity entity, String stateKey, int x, int y, int width, int height,
                          boolean hovered) {
        return renderer.renderCanvas(GuiGraphicsAdapter.unwrap(graphics), entity, stateKey, x, y, width, height, hovered);
    }

    /** 绘制预览底部棋盘格 / Draws the preview checkerboard background. */
    public static void drawCheckerboard(KineticGraphics graphics, int x, int y, int width, int height) {
        EntityPreviewRenderer.drawCheckerboard(GuiGraphicsAdapter.unwrap(graphics), x, y, width, height);
    }

    /** 清空实体缓存 / Clears the entity cache. */
    public void clear() {
        renderer.clear();
    }
}
