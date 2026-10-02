package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public final class KineticSoundEventRegistryRuntime {
    private KineticSoundEventRegistryRuntime() {
    }

    public static KineticRegistryHandle<SoundEvent> register(
            ResourceLocation id,
            Supplier<? extends SoundEvent> factory
    ) {
        return KineticDeferredRegistryRuntime.register(Registries.SOUND_EVENT, id, factory);
    }
}
