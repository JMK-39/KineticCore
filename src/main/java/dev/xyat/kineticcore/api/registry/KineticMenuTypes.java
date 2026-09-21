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
    /** Factory contract used by the enclosing API. */
    @FunctionalInterface
    public interface Factory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory inventory, NetworkBuffer data);
    }

    private KineticMenuTypes() {
    }

    /**
     * Registers this API capability.
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
