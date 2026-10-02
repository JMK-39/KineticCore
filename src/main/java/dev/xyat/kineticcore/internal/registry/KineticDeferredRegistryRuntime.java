package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.runtime.KineticModContextRuntime;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
//? if forge {
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredHolder;
*///?}

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * One DeferredRegister per registry and namespace, attached to the event bus of the mod that registers
 * into that namespace first.
 */
public final class KineticDeferredRegistryRuntime {
    private static final Map<ResourceKey<?>, Map<String, DeferredRegister<?>>> REGISTERS = new LinkedHashMap<>();

    private KineticDeferredRegistryRuntime() {
    }

    @SuppressWarnings("unchecked")
    public static synchronized <R, T extends R> KineticRegistryHandle<T> register(
            ResourceKey<? extends Registry<R>> registryKey,
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        DeferredRegister<R> register = (DeferredRegister<R>) REGISTERS
                .computeIfAbsent(registryKey, key -> new LinkedHashMap<>())
                .computeIfAbsent(id.getNamespace(), namespace -> {
                    DeferredRegister<R> created = DeferredRegister.create(registryKey, namespace);
                    created.register(KineticModContextRuntime.modEventBus());
                    return created;
                });

        //? if forge {
        RegistryObject<T> object = register.register(id.getPath(), factory);
        return new Handle<>(id, object::isPresent, object);
        //?} else {
        /*DeferredHolder<R, T> object = register.register(id.getPath(), factory);
        return new Handle<>(id, object::isBound, object);
        *///?}
    }

    private record Handle<T>(ResourceLocation id, BooleanSupplier present, Supplier<T> value)
            implements KineticRegistryHandle<T> {
        @Override
        public boolean isPresent() {
            return present.getAsBoolean();
        }

        @Override
        public T get() {
            return value.get();
        }
    }
}
