package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticMenuTypes;
import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import dev.xyat.kineticcore.internal.network.NetworkBufferRuntime;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
//? if forge {
import net.minecraftforge.common.extensions.IForgeMenuType;
//?} else {
/*import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
*///?}

public final class KineticMenuTypeRegistryRuntime {
    private KineticMenuTypeRegistryRuntime() {
    }

    public static <T extends AbstractContainerMenu> KineticRegistryHandle<MenuType<T>> register(
            ResourceLocation id,
            KineticMenuTypes.Factory<T> factory
    ) {
        return KineticDeferredRegistryRuntime.register(Registries.MENU, id, () ->
                //? if forge {
                IForgeMenuType.create((containerId, inventory, data) ->
                //?} else {
                /*IMenuTypeExtension.create((containerId, inventory, data) ->
                *///?}
                        factory.create(containerId, inventory, NetworkBufferRuntime.wrapOrEmpty(data))));
    }
}
