package dev.xyat.kineticcore.feature.spawnegg.mixin;

import dev.xyat.kineticcore.feature.spawnegg.event.SpawnEggTemplateHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every egg spawn goes through this overload: placing on a block or fluid, the vanilla dispenser and thrown eggs.
 * Only the throw published a spawn callback, so a special egg placed by hand spawned a plain mob.
 */
@Mixin(EntityType.class)
public abstract class SpawnEggEntityTypeMixin {
    //? if >=26.1 {
    /*@Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private void kineticcore$applySpawnTemplate(ServerLevel level, ItemStack stack, net.minecraft.world.entity.LivingEntity user,
                                                BlockPos pos, MobSpawnType reason, boolean tryMoveDown, boolean movedUp,
                                                CallbackInfoReturnable<Entity> callback) {
        if (stack != null) SpawnEggTemplateHandler.applySpawned(callback.getReturnValue(), stack, user instanceof Player player ? player : null);
    }
    *///?} else {
    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private void kineticcore$applySpawnTemplate(ServerLevel level, ItemStack stack, Player player, BlockPos pos, MobSpawnType reason,
                                                boolean tryMoveDown, boolean movedUp, CallbackInfoReturnable<Entity> callback) {
        if (stack != null) SpawnEggTemplateHandler.applySpawned(callback.getReturnValue(), stack, player);
    }
    //?}
}
