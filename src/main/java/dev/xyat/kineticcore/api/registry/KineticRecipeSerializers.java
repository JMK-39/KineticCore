package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticRecipeSerializerRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticRecipeSerializers {
    private KineticRecipeSerializers() {
    }

    public static <R extends Recipe<?>, T extends RecipeSerializer<R>> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticRecipeSerializerRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static <R extends Recipe<?>, T extends RecipeSerializer<R>> KineticRegistryHandle<T> register(
            String namespace,
            String path,
            Supplier<? extends T> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
