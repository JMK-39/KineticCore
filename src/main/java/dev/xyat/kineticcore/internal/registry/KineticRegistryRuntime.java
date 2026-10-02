package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryView;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
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
//? if >=1.21 {
/*import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
*///?}
//? if forge {
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
//?}

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.Supplier;

public final class KineticRegistryRuntime {
    private static final KineticRegistryView<Item> ITEMS = view(BuiltInRegistries.ITEM);
    private static final KineticRegistryView<EntityType<?>> ENTITY_TYPES = view(BuiltInRegistries.ENTITY_TYPE);
    private static final KineticRegistryView<Block> BLOCKS = view(BuiltInRegistries.BLOCK);
    private static final KineticRegistryView<Fluid> FLUIDS = view(BuiltInRegistries.FLUID);
    private static final KineticRegistryView<MobEffect> MOB_EFFECTS = view(BuiltInRegistries.MOB_EFFECT);
    private static final KineticRegistryView<Attribute> ATTRIBUTES = view(BuiltInRegistries.ATTRIBUTE);
    //? if >=1.21 {
    /*// Enchantments are data-driven since 1.21: they live in the loaded world's registries, not a built-in one.
    private static final KineticRegistryView<Enchantment> ENCHANTMENTS = new View<>(KineticRegistryRuntime::worldEnchantments);
    *///?} else {
    private static final KineticRegistryView<Enchantment> ENCHANTMENTS = view(BuiltInRegistries.ENCHANTMENT);
    //?}
    private static final KineticRegistryView<RecipeType<?>> RECIPE_TYPES = view(BuiltInRegistries.RECIPE_TYPE);
    private static final KineticRegistryView<VillagerProfession> VILLAGER_PROFESSIONS = view(BuiltInRegistries.VILLAGER_PROFESSION);

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
        //? if forge {
        // Forge registries made with RegistryBuilder are not always mirrored into the vanilla root registry.
        IForgeRegistry<?> registry = RegistryManager.ACTIVE.getRegistry(registryId);
        if (registry == null) return Optional.empty();
        return Optional.of(new ForgeView<>((IForgeRegistry<T>) registry));
        //?} else {
        /*Registry<?> registry = BuiltInRegistries.REGISTRY.get(registryId);
        if (registry == null) return Optional.empty();
        return Optional.of(view((Registry<T>) registry));
        *///?}
    }

    private static <T> KineticRegistryView<T> view(Registry<T> registry) {
        return new View<>(() -> registry);
    }

    //? if >=1.21 {
    /*// Outside a world there are no enchantments; the view then behaves like an empty registry.
    private static final Registry<Enchantment> NO_ENCHANTMENTS =
            new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable()).freeze();

    private static Registry<Enchantment> worldEnchantments() {
        RegistryAccess access = KineticRegistryAccessRuntime.current();
        return access == null ? NO_ENCHANTMENTS : access.registryOrThrow(Registries.ENCHANTMENT);
    }
    *///?}

    private record View<T>(Supplier<Registry<T>> source) implements KineticRegistryView<T> {
        @Override
        public T get(ResourceLocation id) {
            return source.get().get(id);
        }

        @Override
        public ResourceLocation id(T value) {
            return source.get().getKey(value);
        }

        @Override
        public Collection<T> values() {
            List<T> result = new ArrayList<>();
            source.get().forEach(result::add);
            return List.copyOf(result);
        }

        @Override
        public Map<ResourceLocation, T> entries() {
            Registry<T> registry = source.get();
            Map<ResourceLocation, T> result = new LinkedHashMap<>();
            for (T value : registry) {
                ResourceLocation id = registry.getKey(value);
                if (id != null) result.put(id, value);
            }
            return Map.copyOf(result);
        }

        @Override
        public Set<ResourceLocation> ids() {
            return Set.copyOf(source.get().keySet());
        }

        @Override
        public boolean contains(ResourceLocation id) {
            return source.get().containsKey(id);
        }

        @Override
        public List<ResourceLocation> tagIds() {
            return source.get().getTagNames().map(TagKey::location).toList();
        }

        @Override
        public List<T> valuesInTag(TagKey<T> tag) {
            List<T> result = new ArrayList<>();
            for (Holder<T> holder : source.get().getTagOrEmpty(tag)) result.add(holder.value());
            return List.copyOf(result);
        }

        @Override
        public boolean isInTag(T value, TagKey<T> tag) {
            return source.get().wrapAsHolder(value).is(tag);
        }

        @Override
        public boolean isInTag(T value, ResourceLocation tagId) {
            if (tagId == null) return false;
            return isInTag(value, TagKey.create(source.get().key(), tagId));
        }

        @Override
        public Holder.Reference<T> holder(T value) {
            Registry<T> registry = source.get();
            return registry.getHolderOrThrow(registry.getResourceKey(value).orElseThrow());
        }
    }

    //? if forge {
    private record ForgeView<T>(IForgeRegistry<T> registry) implements KineticRegistryView<T> {
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
    //?}
}
