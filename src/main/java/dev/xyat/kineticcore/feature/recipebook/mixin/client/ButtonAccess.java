package dev.xyat.kineticcore.feature.recipebook.mixin.client;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ImageButton.class)
public interface ButtonAccess {
    @Accessor("resourceLocation")
    ResourceLocation getBtnTexture();
}
