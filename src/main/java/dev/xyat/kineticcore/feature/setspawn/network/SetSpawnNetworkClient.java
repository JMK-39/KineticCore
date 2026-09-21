package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.setspawn.client.gui.SetSpawnScreen;


public class SetSpawnNetworkClient {
    public static void handleOpenGui(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
        KineticClientRuntime.openScreen(new SetSpawnScreen(packet));
    }

    public static void handleSaveResult(boolean success) {
        net.minecraft.client.gui.screens.Screen current = KineticClientRuntime.currentScreen();
        SetSpawnScreen screen = current instanceof SetSpawnScreen setSpawnScreen ? setSpawnScreen : null;
        if (screen != null) screen.handleSaveResult(success);
    }
}
