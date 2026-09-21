package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import java.util.Objects;
import java.util.function.Function;

public final class ForgeConfigScreenIntegration {
    private ForgeConfigScreenIntegration() {
    }

    public static void installHub(String ownerModId) {
        ModContainer owner = requireContainer(ownerModId);
        owner.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> KTConfigApi.createIndexScreen(parent)
                )
        );
    }

    public static void installOwnerScreen(String ownerModId) {
        ModContainer owner = requireContainer(ownerModId);
        owner.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> KTConfigApi.createScreenForOwner(parent, ownerModId)
                )
        );
    }

    public static void installScreen(String ownerModId, Function<net.minecraft.client.gui.screens.Screen, ? extends net.minecraft.client.gui.screens.Screen> screenFactory) {
        ModContainer owner = requireContainer(ownerModId);
        Function<net.minecraft.client.gui.screens.Screen, ? extends net.minecraft.client.gui.screens.Screen> factory =
                Objects.requireNonNull(screenFactory, "screenFactory");
        owner.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> Objects.requireNonNull(factory.apply(parent), "screenFactory returned null")
                )
        );
    }

    private static ModContainer requireContainer(String ownerModId) {
        if (ownerModId == null || ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId must not be blank");
        }
        return ModList.get().getModContainerById(ownerModId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mod id: " + ownerModId));
    }
}
