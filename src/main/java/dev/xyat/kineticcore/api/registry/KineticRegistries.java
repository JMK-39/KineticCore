package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticRegistryRuntime;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class KineticRegistries {
    private KineticRegistries() {
    }

    public static KineticRegistryView<Item> items() {
        return KineticRegistryRuntime.items();
    }

    public static KineticRegistryView<EntityType<?>> entityTypes() {
        return KineticRegistryRuntime.entityTypes();
    }

    public static KineticRegistryView<Block> blocks() {
        return KineticRegistryRuntime.blocks();
    }

    public static KineticRegistryView<MobEffect> mobEffects() {
        return KineticRegistryRuntime.mobEffects();
    }

    public static KineticRegistryView<Attribute> attributes() {
        return KineticRegistryRuntime.attributes();
    }

    public static KineticRegistryView<Enchantment> enchantments() {
        return KineticRegistryRuntime.enchantments();
    }

    public static KineticRegistryView<RecipeType<?>> recipeTypes() {
        return KineticRegistryRuntime.recipeTypes();
    }

    public static <T> Optional<KineticRegistryView<T>> custom(ResourceLocation registryId) {
        return KineticRegistryRuntime.custom(registryId);
    }
}
