package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.api.network.NetworkBuffer;
import dev.xyat.kineticcore.internal.registry.KineticMenuTypeRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;

public final class KineticMenuTypes {
    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory, NetworkBuffer data);
    }

    private KineticMenuTypes() {
    }

    public static <T extends AbstractContainerMenu> KineticRegistryHandle<MenuType<T>> register(
            ResourceLocation id,
            Factory<T> factory
    ) {
        return KineticMenuTypeRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

    public static <T extends AbstractContainerMenu> KineticRegistryHandle<MenuType<T>> register(
            String namespace,
            String path,
            Factory<T> factory
    ) {
        return register(new ResourceLocation(namespace, path), factory);
    }
}
