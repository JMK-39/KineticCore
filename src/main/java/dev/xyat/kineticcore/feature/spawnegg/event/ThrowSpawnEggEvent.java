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

        //? if >=26.1 {
        /*player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
        *///?} else {
        player.getCooldowns().removeCooldown(stack.getItem());
        //?}
        if (!dev.xyat.kineticcore.feature.spawnegg.SpawnEggLauncher.throwByPlayer(player, stack)) return;

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        //? if >=26.1 {
        /*context.cancellationResult(InteractionResult.SUCCESS);
        *///?} else {
        context.cancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
        //?}
        context.cancel();
    }

    public static ItemStack prepareProjectileStack(ItemStack stack) {
        ItemStack projectileStack = stack.copy();
        projectileStack.setCount(1);
        if (projectileStack.getItem() instanceof SpawnEggItem spawnEgg
                //? if >=1.20.5 {
                /*&& spawnEgg.getType(projectileStack) == EntityType.WARDEN) {
                *///?} else {
                && spawnEgg.getType(projectileStack.getTag()) == EntityType.WARDEN) {
                //?}
            addWardenDigCooldown(projectileStack);
        }

        return projectileStack;
    }

    private static void addWardenDigCooldown(ItemStack stack) {
        //? if >=26.1 {
        /*// 26.1 keeps the entity type next to the data instead of in an id field.
        net.minecraft.world.item.component.TypedEntityData<EntityType<?>> data = stack.get(DataComponents.ENTITY_DATA);
        CompoundTag entityTag = data == null ? new CompoundTag() : data.copyTagWithoutId();
        putDigCooldown(entityTag);
        stack.set(DataComponents.ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.<EntityType<?>>of(EntityType.WARDEN, entityTag));
        *///?} else if >=1.20.5 {
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
