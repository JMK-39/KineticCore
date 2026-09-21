package dev.xyat.kineticcore.feature.spawnegg.network;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.player.LocalPlayer;

public final class SpawnEggNetworkClient {
    private static final String MODE_KEY = "DisableEggThrow";

    private SpawnEggNetworkClient() {
    }

    public static void handleModeSync(boolean disabled) {
        LocalPlayer player = KineticClientRuntime.localPlayer();
        if (player != null) {
            player.getPersistentData().putBoolean(MODE_KEY, disabled);
        }
    }
}
