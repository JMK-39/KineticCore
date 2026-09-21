package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class KineticRecipeSerializerRegistryRuntime {
    private static final Map<String, DeferredRegister<RecipeSerializer<?>>> REGISTRIES = new LinkedHashMap<>();

    private KineticRecipeSerializerRegistryRuntime() {
    }

    public static synchronized <R extends Recipe<?>, T extends RecipeSerializer<R>> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        DeferredRegister<RecipeSerializer<?>> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<RecipeSerializer<?>> created = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<T> object = registry.register(id.getPath(), factory::get);
        return new Handle<>(id, object);
    }

    private record Handle<T>(ResourceLocation id, RegistryObject<T> object) implements KineticRegistryHandle<T> {
        @Override
        public boolean isPresent() {
            return object.isPresent();
        }

        @Override
        public T get() {
            return object.get();
        }
    }
}
