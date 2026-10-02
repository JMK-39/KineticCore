package dev.xyat.kineticcore.internal.client.registry;

import dev.xyat.kineticcore.api.client.registry.KineticClientMenus;
import dev.xyat.kineticcore.internal.client.gui.page.PageContainerScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
//? if forge {
import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;
//?} else {
/*import dev.xyat.kineticcore.internal.runtime.KineticModContextRuntime;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
*///?}

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticClientMenuRuntime {
    private KineticClientMenuRuntime() {
    }

    public static synchronized <M extends AbstractContainerMenu> void register(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.PageFactory<M> factory
    ) {
        // One registration job per menu: a broken factory must not hide others.
        Registration<M> registration = new Registration<>(menuType, factory);
        //? if forge {
        // onClientSetup also rejects late registration before any state is published.
        KineticModLifecycleRuntime.onClientSetup(registration::register);
        //?} else {
        /*// NeoForge only accepts menu screens through RegisterMenuScreensEvent.
        KineticModContextRuntime.modEventBus().addListener((RegisterMenuScreensEvent event) -> registration.register(event));
        *///?}
    }

    private record Registration<M extends AbstractContainerMenu>(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.PageFactory<M> factory
    ) {
        private MenuScreens.ScreenConstructor<M, PageContainerScreen<M>> constructor() {
            return (menu, inventory, title) ->
                    new PageContainerScreen<>(Objects.requireNonNull(factory.create(menu, title),
                            "container page factory returned null"), inventory, title);
        }

        //? if forge {
        private void register() {
            MenuScreens.register(menuType.get(), constructor());
        }
        //?} else {
        /*private void register(RegisterMenuScreensEvent event) {
            event.register(menuType.get(), constructor());
        }
        *///?}
    }
}
