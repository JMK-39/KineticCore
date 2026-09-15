package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticSoundEventRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticSoundEvents {
    private KineticSoundEvents() {
    }

    public static KineticRegistryHandle<SoundEvent> register(
            ResourceLocation id,
            Supplier<? extends SoundEvent> factory
    ) {
        return KineticSoundEventRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static KineticRegistryHandle<SoundEvent> register(
            String namespace,
            String path,
            Supplier<? extends SoundEvent> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
