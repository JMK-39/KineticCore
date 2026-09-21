package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticItemRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for items. */
public final class KineticItems {
    private KineticItems() {
    }

    /**
     * Registers this API capability.
     */
    public static <T extends Item> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticItemRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

}
