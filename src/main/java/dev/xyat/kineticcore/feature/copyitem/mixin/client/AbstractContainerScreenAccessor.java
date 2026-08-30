package dev.xyat.kineticcore.feature.copyitem.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("hoveredSlot")
    Slot kineticcore$getHoveredSlot();

    @Accessor("leftPos")
    int kineticcore$getLeftPos();

    @Accessor("topPos")
    int kineticcore$getTopPos();
}
