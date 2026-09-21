package dev.xyat.kineticcore.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Public API contract for kinetic registry handle. */
public interface KineticRegistryHandle<T> extends Supplier<T> {
    ResourceLocation id();

    /** Returns whether the backing registry object has completed registration and can be read safely. */
    boolean isPresent();

    @Override
    T get();
}
