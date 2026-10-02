package dev.xyat.kineticcore.feature.spawnegg.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.feature.spawnegg.config.SpawnEggConfig;
import dev.xyat.kineticcore.feature.spawnegg.entity.ThrowSpawnEgg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
*///?}

public final class ThrowSpawnEggEvent {
    private static final String MODE_KEY = "DisableEggThrow";
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    private ThrowSpawnEggEvent() {
    }

    public static void register() {
        REGISTRATION.run(() -> KineticPlayerEvents.onRightClickItem(KineticEventPriority.NORMAL, ThrowSpawnEggEvent::throwingSpawn));
    }

    private static void throwingSpawn(KineticPlayerEvents.RightClickItemContext context) {
        if (!SpawnEggConfig.enableSpawnEggThrow) return;

        Player player = context.player();
        if (player.getPersistentData().getBoolean(MODE_KEY)) return;

        ItemStack stack = context.stack();
        if (!(stack.getItem() instanceof SpawnEggItem)) return;

        player.getCooldowns().removeCooldown(stack.getItem());
        throwSpawnEgg(player, stack);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        context.cancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        context.cancel();
    }

    private static void throwSpawnEgg(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;

        ItemStack projectileStack = stack.copy();
        if (projectileStack.getItem() instanceof SpawnEggItem spawnEgg
                //? if >=1.20.5 {
                /*&& spawnEgg.getType(projectileStack) == EntityType.WARDEN) {
                *///?} else {
                && spawnEgg.getType(projectileStack.getTag()) == EntityType.WARDEN) {
                //?}
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
        //? if >=1.20.5 {
        /*// The egg's entity data is the entity_data component since 1.20.5, which must name the entity.
        CustomData.update(DataComponents.ENTITY_DATA, stack, entityTag -> {
            if (!entityTag.contains("id", Tag.TAG_STRING)) entityTag.putString("id", "minecraft:warden");
            putDigCooldown(entityTag);
        });
        *///?} else {
        CompoundTag entityTag = stack.getOrCreateTagElement("EntityTag");
        putDigCooldown(entityTag);
        stack.addTagElement("EntityTag", entityTag);
        //?}
    }

    private static void putDigCooldown(CompoundTag entityTag) {
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
    }
}
