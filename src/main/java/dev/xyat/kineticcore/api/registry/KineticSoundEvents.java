package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticSoundEventRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for sound events. */
public final class KineticSoundEvents {
    private KineticSoundEvents() {
    }

    /**
     * Registers this API capability.
     */
    public static KineticRegistryHandle<SoundEvent> register(
            ResourceLocation id,
            Supplier<? extends SoundEvent> factory
    ) {
        return KineticSoundEventRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

}
