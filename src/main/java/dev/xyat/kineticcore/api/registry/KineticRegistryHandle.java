package dev.xyat.kineticcore.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public interface KineticRegistryHandle<T> extends Supplier<T> {
    ResourceLocation id();

    @Override
    T get();
}
