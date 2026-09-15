package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticMenuTypes;
import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.network.NetworkBufferRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KineticMenuTypeRegistryRuntime {
    private static final Map<String, DeferredRegister<MenuType<?>>> REGISTRIES = new LinkedHashMap<>();

    private KineticMenuTypeRegistryRuntime() {
    }

    public static synchronized <T extends net.minecraft.world.inventory.AbstractContainerMenu> KineticRegistryHandle<MenuType<T>> register(
            ResourceLocation id,
            KineticMenuTypes.Factory<T> factory
    ) {
        DeferredRegister<MenuType<?>> registry = REGISTRIES.computeIfAbsent(id.getNamespace(), namespace -> {
            DeferredRegister<MenuType<?>> created = DeferredRegister.create(ForgeRegistries.MENU_TYPES, namespace);
            created.register(FMLJavaModLoadingContext.get().getModEventBus());
            return created;
        });

        RegistryObject<MenuType<T>> object = registry.register(
                id.getPath(),
                () -> IForgeMenuType.create((containerId, inventory, data) ->
                        factory.create(containerId, inventory, NetworkBufferRuntime.wrap(data)))
        );
        return new Handle<>(id, object);
    }

    private record Handle<T>(ResourceLocation id, RegistryObject<T> object) implements KineticRegistryHandle<T> {
        @Override
        public T get() {
            return object.get();
        }
    }
}
