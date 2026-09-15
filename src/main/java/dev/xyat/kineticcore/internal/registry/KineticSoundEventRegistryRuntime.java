package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class KineticSoundEventRegistryRuntime {
    private static final Map<String, DeferredRegister<SoundEvent>> REGISTRIES = new LinkedHashMap<>();

    private KineticSoundEventRegistryRuntime() {
    }

    public static synchronized KineticRegistryHandle<SoundEvent> register(
            ResourceLocation id,
            Supplier<? extends SoundEvent> factory
    ) {
        DeferredRegister<SoundEvent> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<SoundEvent> created = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<SoundEvent> object = registry.register(id.getPath(), factory::get);
        return new Handle(id, object);
    }

    private record Handle(ResourceLocation id, RegistryObject<SoundEvent> object)
            implements KineticRegistryHandle<SoundEvent> {
        @Override
        public SoundEvent get() {
            return object.get();
        }
    }
}
