package dev.xyat.kineticcore.feature.spawnegg.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SpawnEggNetworkClient {
    private static final String MODE_KEY = "DisableEggThrow";

    private SpawnEggNetworkClient() {
    }

    public static void handleModeSync(boolean disabled) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getPersistentData().putBoolean(MODE_KEY, disabled);
        }
    }
}
