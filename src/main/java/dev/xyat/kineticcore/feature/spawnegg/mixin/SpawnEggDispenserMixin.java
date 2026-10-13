package dev.xyat.kineticcore.feature.spawnegg.mixin;

import dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData;
import dev.xyat.kineticcore.feature.spawnegg.SpawnEggDispenseBehavior;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class SpawnEggDispenserMixin {
    @Inject(method = "getDispenseMethod", at = @At("HEAD"), cancellable = true)
    //? if >=1.21 {
    /*private void kineticcore$specialEgg(Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (!stack.isItemEnabled(level.enabledFeatures())) return;
    *///?} else {
    private void kineticcore$specialEgg(ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
    //?}
        if (SpawnEggConfig.enableSpawnEggThrow && stack.getItem() instanceof SpawnEggItem && ItemCreatorStackData.hasTemplate(stack))
            cir.setReturnValue(SpawnEggDispenseBehavior.INSTANCE);
    }
}
