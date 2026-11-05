package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.feature.setspawn.client.gui.SetSpawnPage;


public class SetSpawnNetworkClient {
    public static void handleOpenGui(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
        // Back returns to the page that asked for the editor (the spawn settings), not to the game.
        KineticGui.openChild(new SetSpawnPage(packet));
    }

    public static void handleSaveResult(boolean success) {
        SetSpawnPage page = KineticGui.currentPage(SetSpawnPage.class);
        if (page != null) page.handleSaveResult(success);
    }
}
