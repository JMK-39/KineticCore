package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticItemRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticItems {
    private KineticItems() {
    }

    public static <T extends Item> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticItemRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static <T extends Item> KineticRegistryHandle<T> register(
            String namespace,
            String path,
            Supplier<? extends T> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
