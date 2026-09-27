package dev.xyat.kineticcore.internal.client.registry;

import dev.xyat.kineticcore.api.client.registry.KineticClientMenus;
import dev.xyat.kineticcore.internal.client.gui.page.PageContainerScreen;
import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticClientMenuRuntime {
    private KineticClientMenuRuntime() {
    }

    public static synchronized <M extends AbstractContainerMenu> void register(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.PageFactory<M> factory
    ) {
        // Queue one Forge setup job per menu: a broken factory must not hide others.
        // onClientSetup also rejects late registration before any state is published.
        Registration<M> registration = new Registration<>(menuType, factory);
        KineticModLifecycleRuntime.onClientSetup(registration::register);
    }

    private record Registration<M extends AbstractContainerMenu>(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.PageFactory<M> factory
    ) {
        private void register() {
            MenuScreens.<M, PageContainerScreen<M>>register(menuType.get(), (menu, inventory, title) ->
                    new PageContainerScreen<>(Objects.requireNonNull(factory.create(menu, title),
                            "container page factory returned null"), inventory, title));
        }
    }
}
