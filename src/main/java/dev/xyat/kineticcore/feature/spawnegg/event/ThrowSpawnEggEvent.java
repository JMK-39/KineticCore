package dev.xyat.kineticcore.feature.spawnegg.event;

import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@KTModule
public final class ThrowSpawnEggEvent {
    private static final String MODE_KEY = "DisableEggThrow";
    private static boolean registered;

    private ThrowSpawnEggEvent() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(ThrowSpawnEggEvent::throwingSpawn);
    }

    private static void throwingSpawn(PlayerInteractEvent.RightClickItem event) {
        if (!SpawnEggConfig.enableSpawnEggThrow) return;

        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean(MODE_KEY)) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SpawnEggItem)) return;

        player.getCooldowns().removeCooldown(stack.getItem());
        throwSpawnEgg(player, stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        event.setCanceled(true);
    }

    private static void throwSpawnEgg(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;

        ItemStack projectileStack = stack.copy();
        if (projectileStack.getItem() instanceof SpawnEggItem spawnEgg
                && spawnEgg.getType(projectileStack.getTag()) == EntityType.WARDEN) {
            addWardenDigCooldown(projectileStack);
        }

        ThrowSpawnEgg projectile = new ThrowSpawnEgg(player.level(), player);
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                (float) SpawnEggConfig.spawnEggThrowSpeed,
                (float) SpawnEggConfig.spawnEggThrowInaccuracy
        );
        player.level().addFreshEntity(projectile);
    }

    private static void addWardenDigCooldown(ItemStack stack) {
        CompoundTag entityTag = stack.getOrCreateTagElement("EntityTag");
        CompoundTag brain = entityTag.contains("Brain", Tag.TAG_COMPOUND)
                ? entityTag.getCompound("Brain")
                : new CompoundTag();
        CompoundTag memories = brain.contains("memories", Tag.TAG_COMPOUND)
                ? brain.getCompound("memories")
                : new CompoundTag();
        CompoundTag cooldown = new CompoundTag();
        cooldown.put("value", new CompoundTag());
        cooldown.putLong("ttl", 1200L);
        memories.put("minecraft:dig_cooldown", cooldown);
        brain.put("memories", memories);
        entityTag.put("Brain", brain);
        stack.addTagElement("EntityTag", entityTag);
    }
}
