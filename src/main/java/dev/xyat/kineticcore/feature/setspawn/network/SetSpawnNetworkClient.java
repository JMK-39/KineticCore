package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.setspawn.client.gui.SetSpawnScreen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SetSpawnNetworkClient {
    public static void handleOpenGui(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
        KineticClientRuntime.openScreen(() -> new SetSpawnScreen(packet));
    }

    public static void handleSaveResult(boolean success) {
        SetSpawnScreen screen = KineticClientRuntime.currentScreen(SetSpawnScreen.class);
        if (screen != null) screen.handleSaveResult(success);
    }
}
