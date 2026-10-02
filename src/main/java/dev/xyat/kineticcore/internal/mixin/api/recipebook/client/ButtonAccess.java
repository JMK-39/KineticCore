package dev.xyat.kineticcore.internal.mixin.api.recipebook.client;

import net.minecraft.client.gui.components.ImageButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
//? if >=1.20.2 {
/*import net.minecraft.client.gui.components.WidgetSprites;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

@Mixin(ImageButton.class)
public interface ButtonAccess {
    // Image buttons draw a sprite set instead of one texture since 1.20.2.
    //? if >=1.20.2 {
    /*@Accessor("sprites")
    WidgetSprites getBtnSprites();
    *///?} else {
    @Accessor("resourceLocation")
    ResourceLocation getBtnTexture();
    //?}
}
