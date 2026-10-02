package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

public final class KineticEntityTypeRegistryRuntime {
    private KineticEntityTypeRegistryRuntime() {
    }

    public static <T extends Entity> KineticRegistryHandle<EntityType<T>> register(
            ResourceLocation id,
            Supplier<? extends EntityType<T>> factory
    ) {
        return KineticDeferredRegistryRuntime.register(Registries.ENTITY_TYPE, id, factory);
    }
}
