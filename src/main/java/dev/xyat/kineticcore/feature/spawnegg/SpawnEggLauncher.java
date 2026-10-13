package dev.xyat.kineticcore.feature.spawnegg;

import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import dev.xyat.kineticcore.feature.spawnegg.event.ThrowSpawnEggEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class SpawnEggLauncher {
    private SpawnEggLauncher() { }
    public static boolean throwByPlayer(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return true;
        ThrowSpawnEgg projectile = new ThrowSpawnEgg(player.level(), player);
        projectile.setItem(ThrowSpawnEggEvent.prepareProjectileStack(stack));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0,
                (float) SpawnEggConfig.spawnEggThrowSpeed, (float) SpawnEggConfig.spawnEggThrowInaccuracy);
        return player.level().addFreshEntity(projectile);
    }
    public static boolean dispense(ServerLevel level, Vec3 position, Vec3 direction, ItemStack stack) {
        ThrowSpawnEgg projectile = new ThrowSpawnEgg(SpawnEggInit.THROWABLE_SPAWN_EGG.get(), level);
        projectile.setPos(position.x, position.y, position.z);
        projectile.setItem(ThrowSpawnEggEvent.prepareProjectileStack(stack));
        projectile.shoot(direction.x, direction.y, direction.z,
                (float) SpawnEggConfig.spawnEggThrowSpeed, (float) SpawnEggConfig.spawnEggThrowInaccuracy);
        return level.addFreshEntity(projectile);
    }
}
