package dev.xyat.kineticcore.internal.mixin.api.minecraft.client;

//? if >=26.1 {
/*import dev.xyat.kineticcore.internal.client.gui.page.ContainerScreenSizeAccess;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

// 26.1 的容器界面在构造时固定尺寸，页面布局需要在之后调整 / 26.1 container screens fix their size in the constructor;
// page layouts change it afterwards. Older versions list it in unavailable_mixins.
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenSizeAccessor extends ContainerScreenSizeAccess {
    @Override
    @Mutable
    @Accessor("imageWidth")
    void kineticcore$setImageWidth(int width);

    @Override
    @Mutable
    @Accessor("imageHeight")
    void kineticcore$setImageHeight(int height);
}
*///?}
