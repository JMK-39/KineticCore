package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class KineticEnchantmentRegistryRuntime {
    private static final Map<String, DeferredRegister<Enchantment>> REGISTRIES = new LinkedHashMap<>();

    private KineticEnchantmentRegistryRuntime() {
    }

    public static synchronized <T extends Enchantment> KineticRegistryHandle<T> register(
            ResourceLocation id,
            Supplier<? extends T> factory
    ) {
        DeferredRegister<Enchantment> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<Enchantment> created = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<T> object = registry.register(id.getPath(), factory::get);
        return new Handle<>(id, object);
    }

    private record Handle<T>(ResourceLocation id, RegistryObject<T> object) implements KineticRegistryHandle<T> {
        @Override
        public T get() {
            return object.get();
        }
    }
}
