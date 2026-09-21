package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticRegistryRuntime;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.Block;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Public Kinetic API facade for registries. */
public final class KineticRegistries {
    private KineticRegistries() {
    }

    /**
     * Performs the items API operation.
     */
    public static KineticRegistryView<Item> items() {
        return KineticRegistryRuntime.items();
    }

    /**
     * Performs the entity types API operation.
     */
    public static KineticRegistryView<EntityType<?>> entityTypes() {
        return KineticRegistryRuntime.entityTypes();
    }

    /**
     * Performs the blocks API operation.
     */
    public static KineticRegistryView<Block> blocks() {
        return KineticRegistryRuntime.blocks();
    }

    /**
     * Performs the fluids API operation.
     */
    public static KineticRegistryView<Fluid> fluids() {
        return KineticRegistryRuntime.fluids();
    }

    /**
     * Performs the mob effects API operation.
     */
    public static KineticRegistryView<MobEffect> mobEffects() {
        return KineticRegistryRuntime.mobEffects();
    }

    /**
     * Performs the attributes API operation.
     */
    public static KineticRegistryView<Attribute> attributes() {
        return KineticRegistryRuntime.attributes();
    }

    /**
     * Performs the enchantments API operation.
     */
    public static KineticRegistryView<Enchantment> enchantments() {
        return KineticRegistryRuntime.enchantments();
    }

    /**
     * Performs the recipe types API operation.
     */
    public static KineticRegistryView<RecipeType<?>> recipeTypes() {
        return KineticRegistryRuntime.recipeTypes();
    }

    /**
     * Performs the villager professions API operation.
     */
    public static KineticRegistryView<VillagerProfession> villagerProfessions() {
        return KineticRegistryRuntime.villagerProfessions();
    }

    /**
     * Performs the custom API operation.
     */
    public static <T> Optional<KineticRegistryView<T>> custom(ResourceLocation registryId) {
        return KineticRegistryRuntime.custom(registryId);
    }
}
