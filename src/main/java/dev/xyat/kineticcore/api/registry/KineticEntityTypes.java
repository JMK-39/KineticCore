package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticEntityTypeRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticEntityTypes {
    private KineticEntityTypes() {
    }

    public static <T extends Entity> KineticRegistryHandle<EntityType<T>> register(
            ResourceLocation id,
            Supplier<? extends EntityType<T>> factory
    ) {
        return KineticEntityTypeRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static <T extends Entity> KineticRegistryHandle<EntityType<T>> register(
            String namespace,
            String path,
            Supplier<? extends EntityType<T>> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
