package dev.xyat.kineticcore.internal.client.config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
//? if forge {
import net.minecraftforge.client.ConfigScreenHandler;
//?} else {
/*import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
*///?}

import java.util.Objects;
import java.util.function.Function;

public final class ForgeConfigScreenIntegration {
    private ForgeConfigScreenIntegration() {
    }

    public static void installHub(String ownerModId) {
        register(requireContainer(ownerModId), parent -> ConfigScreens.createIndex());
    }

    public static void installScreen(String ownerModId, Function<Screen, ? extends Screen> screenFactory) {
        ModContainer owner = requireContainer(ownerModId);
        Function<Screen, ? extends Screen> factory = Objects.requireNonNull(screenFactory, "screenFactory");
        register(owner, parent -> Objects.requireNonNull(factory.apply(parent), "screenFactory returned null"));
    }

    // Shows the screen when the mod's "Config" button in the mod list is pressed.
    private static void register(ModContainer owner, Function<Screen, Screen> screen) {
        //? if forge {
        owner.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> screen.apply(parent))
        );
        //?} else {
        /*owner.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> screen.apply(parent));
        *///?}
    }

    private static ModContainer requireContainer(String ownerModId) {
        if (ownerModId == null || ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId must not be blank");
        }
        return ModList.get().getModContainerById(ownerModId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mod id: " + ownerModId));
    }
}
