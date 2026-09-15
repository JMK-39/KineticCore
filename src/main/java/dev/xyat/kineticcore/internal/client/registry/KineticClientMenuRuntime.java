package dev.xyat.kineticcore.internal.client.registry;

import dev.xyat.kineticcore.api.client.registry.KineticClientMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class KineticClientMenuRuntime {
    private static final List<Registration<?, ?>> REGISTRATIONS = new ArrayList<>();
    private static boolean listenerRegistered;

    private KineticClientMenuRuntime() {
    }

    public static synchronized <M extends AbstractContainerMenu, S extends AbstractContainerScreen<M>> void register(
            Supplier<? extends MenuType<M>> menuType,
            KineticClientMenus.ScreenFactory<M, S> factory
    ) {
        REGISTRATIONS.add(new Registration<>(menuType, factory));
        ensureListener();
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticClientMenuRuntime::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        List<Registration<?, ?>> registrations;
        synchronized (KineticClientMenuRuntime.class) {
            registrations = List.copyOf(REGISTRATIONS);
        }
        event.enqueueWork(() -> registrations.forEach(Registration::register));
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
