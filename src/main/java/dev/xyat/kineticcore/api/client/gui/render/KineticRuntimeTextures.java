package dev.xyat.kineticcore.api.client.gui.render;

import dev.xyat.kineticcore.internal.client.gui.render.RuntimeTextures;

import javax.annotation.Nullable;

/**
 * 运行时生成的 GUI 贴图（例如结构缩略图）：PNG 数据上传为可绘制的贴图，缩放时平滑。只能在渲染线程调用。
 * GUI textures made at runtime (for example structure thumbnails): PNG data uploaded as a drawable texture that is
 * smoothed when scaled. Render thread only.
 */
public final class KineticRuntimeTextures {
    private KineticRuntimeTextures() {
    }

    /**
     * 把 PNG 上传为贴图 {@code namespace:path}，替换同名旧贴图；数据不是 PNG 时返回 {@code null}。
     * Uploads a PNG as the texture {@code namespace:path}, replacing an earlier one of that name. Returns the texture,
     * sized as the image, or {@code null} if the data is not a readable PNG.
     */
    @Nullable
    public static KineticTexture upload(String namespace, String path, byte[] png) {
        return RuntimeTextures.upload(namespace, path, png);
    }

    /** 释放 {@link #upload} 生成的贴图 / Frees a texture made by {@link #upload}. */
    public static void release(KineticTexture texture) {
        RuntimeTextures.release(texture);
    }
}
