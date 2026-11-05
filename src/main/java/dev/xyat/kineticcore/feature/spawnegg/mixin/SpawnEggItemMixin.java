package dev.xyat.kineticcore.feature.spawnegg.mixin;

import dev.xyat.kineticcore.feature.spawnegg.NativeSpawnEggData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Inject(method = "useOn", at = @At(value = "INVOKE",
            //? if >=1.20.5 {
            /*target = "Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V",
            *///?} else {
            target = "Lnet/minecraft/world/level/block/entity/SpawnerBlockEntity;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V",
            //?}
            shift = At.Shift.AFTER))
    private void kineticcore$writeSpawnerTemplate(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        if (context.getLevel() instanceof ServerLevel level
                && level.getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity spawner) {
            NativeSpawnEggData.configure(spawner, context.getItemInHand(), level, context.getPlayer());
        }
    }
}
