package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

public final class KineticClientRuntime {
    private KineticClientRuntime() {
    }

    public static void execute(Runnable action) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.execute(action);
    }

    public static Screen currentScreen() {
        KineticClientRuntimeImpl.initialize();
        return KineticClientRuntimeImpl.currentScreen();
    }

    public static <T extends Screen> T currentScreen(Class<T> type) {
        Screen screen = currentScreen();
        return type.isInstance(screen) ? type.cast(screen) : null;
    }

    public static void openScreen(Screen screen) {
        KineticClientRuntimeImpl.initialize();
        KineticClientRuntimeImpl.openScreen(screen);
    }

    public static void openScreen(Supplier<? extends Screen> screenFactory) {
        if (screenFactory == null) return;
        openScreen(screenFactory.get());
    }
}
