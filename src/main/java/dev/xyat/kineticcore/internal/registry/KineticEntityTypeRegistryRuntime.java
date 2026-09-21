package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class KineticEntityTypeRegistryRuntime {
    private static final Map<String, DeferredRegister<EntityType<?>>> REGISTRIES = new LinkedHashMap<>();

    private KineticEntityTypeRegistryRuntime() {
    }

    public static synchronized <T extends Entity> KineticRegistryHandle<EntityType<T>> register(
            ResourceLocation id,
            Supplier<? extends EntityType<T>> factory
    ) {
        DeferredRegister<EntityType<?>> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<EntityType<?>> created = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<EntityType<T>> object = registry.register(id.getPath(), factory::get);
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
