package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.menu.KineticMenus;
import dev.xyat.kineticcore.api.network.NetworkBuffer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Consumer;

public final class KineticMenuRuntime {
    private KineticMenuRuntime() {
    }

    public static void open(
            ServerPlayer player,
            Component title,
            KineticMenus.MenuFactory factory,
            Consumer<NetworkBuffer> extraDataWriter
    ) {
        SimpleMenuProvider provider = new SimpleMenuProvider(factory::create, title);
        if (extraDataWriter == null) {
            NetworkHooks.openScreen(player, provider);
            return;
        }
        NetworkHooks.openScreen(player, provider, buffer -> extraDataWriter.accept(new ForgeNetworkBuffer(buffer)));
    }
}
