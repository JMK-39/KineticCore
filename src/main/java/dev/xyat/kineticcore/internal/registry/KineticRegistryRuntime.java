package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryView;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

public final class KineticRegistryRuntime {
    private static final KineticRegistryView<Item> ITEMS = new View<>(ForgeRegistries.ITEMS);
    private static final KineticRegistryView<EntityType<?>> ENTITY_TYPES = new View<>(ForgeRegistries.ENTITY_TYPES);
    private static final KineticRegistryView<Block> BLOCKS = new View<>(ForgeRegistries.BLOCKS);
    private static final KineticRegistryView<Fluid> FLUIDS = new View<>(ForgeRegistries.FLUIDS);
    private static final KineticRegistryView<MobEffect> MOB_EFFECTS = new View<>(ForgeRegistries.MOB_EFFECTS);
    private static final KineticRegistryView<Attribute> ATTRIBUTES = new View<>(ForgeRegistries.ATTRIBUTES);
    private static final KineticRegistryView<Enchantment> ENCHANTMENTS = new View<>(ForgeRegistries.ENCHANTMENTS);
    private static final KineticRegistryView<RecipeType<?>> RECIPE_TYPES = new View<>(ForgeRegistries.RECIPE_TYPES);
    private static final KineticRegistryView<VillagerProfession> VILLAGER_PROFESSIONS = new View<>(ForgeRegistries.VILLAGER_PROFESSIONS);

    private KineticRegistryRuntime() {
    }

    public static KineticRegistryView<Item> items() {
        return ITEMS;
    }

    public static KineticRegistryView<EntityType<?>> entityTypes() {
        return ENTITY_TYPES;
    }

    public static KineticRegistryView<Block> blocks() {
        return BLOCKS;
    }

    public static KineticRegistryView<Fluid> fluids() {
        return FLUIDS;
    }

    public static KineticRegistryView<MobEffect> mobEffects() {
        return MOB_EFFECTS;
    }

    public static KineticRegistryView<Attribute> attributes() {
        return ATTRIBUTES;
    }

    public static KineticRegistryView<Enchantment> enchantments() {
        return ENCHANTMENTS;
    }

    public static KineticRegistryView<RecipeType<?>> recipeTypes() {
        return RECIPE_TYPES;
    }

    public static KineticRegistryView<VillagerProfession> villagerProfessions() {
        return VILLAGER_PROFESSIONS;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<KineticRegistryView<T>> custom(ResourceLocation registryId) {
        if (registryId == null) return Optional.empty();
        IForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(registryId);
        if (registry == null) return Optional.empty();
        return Optional.of(new View<>((IForgeRegistry<T>) registry));
    }

    private static final class View<T> implements KineticRegistryView<T> {
        private final IForgeRegistry<T> registry;

        private View(IForgeRegistry<T> registry) {
            this.registry = registry;
        }

        @Override
        public T get(ResourceLocation id) {
            return registry.getValue(id);
        }

        @Override
        public ResourceLocation id(T value) {
            return registry.getKey(value);
        }

        @Override
        public Collection<T> values() {
            return List.copyOf(registry.getValues());
        }

        @Override
        public Map<ResourceLocation, T> entries() {
            Map<ResourceLocation, T> result = new LinkedHashMap<>();
            for (T value : registry.getValues()) {
                ResourceLocation id = registry.getKey(value);
                if (id != null) result.put(id, value);
            }
            return Map.copyOf(result);
        }

        @Override
        public Set<ResourceLocation> ids() {
            return Set.copyOf(registry.getKeys());
        }

        @Override
        public boolean contains(ResourceLocation id) {
            return registry.containsKey(id);
        }

        @Override
        public List<ResourceLocation> tagIds() {
            var manager = registry.tags();
            if (manager == null) return List.of();
            return manager.getTagNames().map(TagKey::location).toList();
        }

        @Override
        public List<T> valuesInTag(TagKey<T> tag) {
            var manager = registry.tags();
            if (manager == null) return List.of();
            return manager.getTag(tag).stream().toList();
        }

        @Override
        public boolean isInTag(T value, TagKey<T> tag) {
            var manager = registry.tags();
            return manager != null && manager.getTag(tag).contains(value);
        }

        @Override
        public boolean isInTag(T value, ResourceLocation tagId) {
            if (tagId == null) return false;
            return isInTag(value, TagKey.create(registry.getRegistryKey(), tagId));
        }

        @Override
        public Holder.Reference<T> holder(T value) {
            return registry.getDelegateOrThrow(value);
        }
    }
}
