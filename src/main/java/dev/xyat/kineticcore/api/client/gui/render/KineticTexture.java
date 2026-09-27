package dev.xyat.kineticcore.api.client.gui.render;

import java.util.Objects;

/**
 * 一张 GUI 贴图及其像素尺寸。附属只用命名空间与路径描述贴图，不接触原版资源标识类型。
 * A GUI texture and its pixel size. Addons describe textures by namespace and path only, without touching the
 * vanilla resource-identifier type (renamed between Minecraft versions).
 */
public record KineticTexture(String namespace, String path, int textureWidth, int textureHeight) {
    /** 校验字段 / Validates fields. */
    public KineticTexture {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isBlank() || path.isBlank()) throw new IllegalArgumentException("texture namespace and path must not be blank");
        if (textureWidth <= 0 || textureHeight <= 0) throw new IllegalArgumentException("texture size must be positive");
    }

    /** 256×256 贴图 / A 256×256 texture. */
    public static KineticTexture of(String namespace, String path) {
        return new KineticTexture(namespace, path, 256, 256);
    }

    /** 指定尺寸的贴图 / A texture of the given size. */
    public static KineticTexture of(String namespace, String path, int textureWidth, int textureHeight) {
        return new KineticTexture(namespace, path, textureWidth, textureHeight);
    }
}
