package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

public final class ForgeConfigScreenIntegration {
    private ForgeConfigScreenIntegration() {
    }

    public static void installHub(String ownerModId) {
        ModContainer owner = requireContainer(ownerModId);
        owner.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> KTConfigApi.createScreen(parent)
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

    private static ModContainer requireContainer(String ownerModId) {
        if (ownerModId == null || ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId must not be blank");
        }
        return ModList.get().getModContainerById(ownerModId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown mod id: " + ownerModId));
    }
}
