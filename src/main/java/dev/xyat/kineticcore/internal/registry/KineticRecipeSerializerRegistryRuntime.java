package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Supplier;

public final class KineticRecipeSerializerRegistryRuntime {
    private KineticRecipeSerializerRegistryRuntime() {
    }

    public static <R extends Recipe<?>, T extends RecipeSerializer<R>> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticDeferredRegistryRuntime.register(Registries.RECIPE_SERIALIZER, id, factory);
    }
}
