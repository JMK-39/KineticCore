package dev.xyat.kineticcore.internal.client.gui.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.xyat.kineticcore.api.client.gui.render.KineticTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

import javax.annotation.Nullable;
import java.io.IOException;

/** Uploads PNG data as GUI textures at runtime and frees them again; render thread only. */
public final class RuntimeTextures {
    private RuntimeTextures() {
    }

    @Nullable
    public static KineticTexture upload(String namespace, String path, byte[] png) {
        NativeImage image;
        try {
            image = NativeImage.read(png);
        } catch (IOException | RuntimeException e) {
            return null;
        }
        KineticTexture texture = KineticTexture.of(namespace, path, image.getWidth(), image.getHeight());
        //? if <26.1 {
        DynamicTexture dynamic = new DynamicTexture(image);
        // Pictures are shown smaller or larger than drawn; smoothing keeps them from looking ragged.
        dynamic.setFilter(true, false);
        //?} else {
        /*DynamicTexture dynamic = new DynamicTexture(() -> "kinetic " + path, image) {
            {
                // Pictures are shown smaller or larger than drawn; smoothing keeps them from looking ragged.
                this.sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                        .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
        *///?}
        Minecraft.getInstance().getTextureManager().register(GuiGraphicsAdapter.location(texture), dynamic);
        return texture;
    }

    public static void release(KineticTexture texture) {
        Minecraft.getInstance().getTextureManager().release(GuiGraphicsAdapter.location(texture));
    }
}
