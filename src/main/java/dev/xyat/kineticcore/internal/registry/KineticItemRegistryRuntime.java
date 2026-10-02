package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class KineticItemRegistryRuntime {
    private KineticItemRegistryRuntime() {
    }

    public static <T extends Item> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        return KineticDeferredRegistryRuntime.register(Registries.ITEM, id, factory);
    }
}
