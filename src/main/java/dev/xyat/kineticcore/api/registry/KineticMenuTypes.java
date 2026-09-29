package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.api.network.NetworkBuffer;
import dev.xyat.kineticcore.internal.registry.KineticMenuTypeRegistryRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;

/** Public Kinetic API facade for menu types. */
public final class KineticMenuTypes {
    /**
     * Creates the client-side menu instance when the server opens a menu of this type.
     *
     * @param <T> menu type
     */
    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu> {
        /**
         * Creates the menu on the client.
         *
         * @param containerId container id assigned by the server
         * @param inventory local player's inventory
         * @param data extra data the server wrote when it opened the menu through {@code KineticMenus}; empty when
         *   it wrote none or the menu was opened without KineticMenus
         * @return the new menu
         */
        T create(int containerId, Inventory inventory, NetworkBuffer data);
    }

    private KineticMenuTypes() {
    }

    /**
     * Registers a menu type whose client instance is created by {@code factory}. Call it from the mod constructor.
     *
     * @param id menu type id
     * @param factory client-side menu factory
     * @param <T> menu type
     * @return a handle that resolves after registration
     * @throws NullPointerException if an argument is {@code null}
     */
    public static <T extends AbstractContainerMenu> KineticRegistryHandle<MenuType<T>> register(
            ResourceLocation id,
            Factory<T> factory
    ) {
        return KineticMenuTypeRegistryRuntime.register(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(factory, "factory")
        );
    }

}
