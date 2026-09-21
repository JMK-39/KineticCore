package dev.xyat.kineticcore.internal.client.registry;

import dev.xyat.kineticcore.api.client.registry.KineticClientMenus;
import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

public final class KineticClientMenuRuntime {
    private KineticClientMenuRuntime() {
    }

    public static synchronized <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void register(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.ScreenFactory<M, S> factory
    ) {
        // Queue one Forge setup job per menu: a broken factory must not hide others.
        // onClientSetup also rejects late registration before any state is published.
        Registration<M, S> registration = new Registration<>(menuType, factory);
        KineticModLifecycleRuntime.onClientSetup(registration::register);
    }

    private record Registration<M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>>(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.ScreenFactory<M, S> factory
    ) {
        private void register() {
            MenuScreens.register(menuType.get(), factory::create);
        }
    }
}
