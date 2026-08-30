package dev.xyat.kineticcore.feature.setspawn.network;

import dev.xyat.kineticcore.feature.setspawn.client.gui.SetSpawnScreen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SetSpawnNetworkClient {
    public static void handleOpenGui(SetSpawnNetwork.OpenSetSpawnGuiPacket packet) {
        Minecraft.getInstance().setScreen(new SetSpawnScreen(packet));
    }

    public static void handleSaveResult(boolean success) {
        if (Minecraft.getInstance().screen instanceof SetSpawnScreen screen) {
            screen.handleSaveResult(success);
        }
    }
}
