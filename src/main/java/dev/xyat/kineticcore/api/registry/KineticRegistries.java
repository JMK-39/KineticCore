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

/**
 * Read-only access to the common game registries without touching Forge or vanilla registry classes directly.
 *
 * <p>Views are shared singletons backed by the live registries, so lookups always see the current content. Registry
 * contents are frozen after mod loading; reading them is safe from any thread, while tag queries reflect the tags
 * of the most recent data-pack reload.
 */
public final class KineticRegistries {
    private KineticRegistries() {
    }

    /**
     * Returns a read-only view of the item registry.
     *
     * @return the shared item view; never {@code null}
     */
    public static KineticRegistryView<Item> items() {
        return KineticRegistryRuntime.items();
    }

    /**
     * Returns a read-only view of the entity type registry.
     *
     * @return the shared entity type view; never {@code null}
     */
    public static KineticRegistryView<EntityType<?>> entityTypes() {
        return KineticRegistryRuntime.entityTypes();
    }

    /**
     * Returns a read-only view of the block registry.
     *
     * @return the shared block view; never {@code null}
     */
    public static KineticRegistryView<Block> blocks() {
        return KineticRegistryRuntime.blocks();
    }

    /**
     * Returns a read-only view of the fluid registry.
     *
     * @return the shared fluid view; never {@code null}
     */
    public static KineticRegistryView<Fluid> fluids() {
        return KineticRegistryRuntime.fluids();
    }

    /**
     * Returns a read-only view of the mob effect registry.
     *
     * @return the shared mob effect view; never {@code null}
     */
    public static KineticRegistryView<MobEffect> mobEffects() {
        return KineticRegistryRuntime.mobEffects();
    }

    /**
     * Returns a read-only view of the attribute registry.
     *
     * @return the shared attribute view; never {@code null}
     */
    public static KineticRegistryView<Attribute> attributes() {
        return KineticRegistryRuntime.attributes();
    }

    /**
     * Returns a read-only view of the enchantment registry.
     *
     * @return the shared enchantment view; never {@code null}
     */
    public static KineticRegistryView<Enchantment> enchantments() {
        return KineticRegistryRuntime.enchantments();
    }

    /**
     * Returns a read-only view of the recipe type registry.
     *
     * @return the shared recipe type view; never {@code null}
     */
    public static KineticRegistryView<RecipeType<?>> recipeTypes() {
        return KineticRegistryRuntime.recipeTypes();
    }

    /**
     * Returns a read-only view of the villager profession registry.
     *
     * @return the shared villager profession view; never {@code null}
     */
    public static KineticRegistryView<VillagerProfession> villagerProfessions() {
        return KineticRegistryRuntime.villagerProfessions();
    }

    /**
     * Returns a view of any Forge-managed registry by id, including registries added by other mods.
     *
     * @param registryId registry id, for example {@code minecraft:block}; {@code null} yields an empty result
     * @param <T> registry element type; the caller must match the registry's actual type
     * @return the view, or empty when no such registry exists
     */
    public static <T> Optional<KineticRegistryView<T>> custom(ResourceLocation registryId) {
        return KineticRegistryRuntime.custom(registryId);
    }
}
