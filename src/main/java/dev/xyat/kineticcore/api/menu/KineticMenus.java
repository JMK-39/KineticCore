package dev.xyat.kineticcore.api.menu;

import dev.xyat.kineticcore.api.network.NetworkBuffer;
import dev.xyat.kineticcore.internal.network.KineticMenuRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;
import java.util.function.Consumer;

public final class KineticMenus {
    @FunctionalInterface
    public interface MenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory, Player player);
    }

    private KineticMenus() {
    }

    public static void open(ServerPlayer player, Component title, MenuFactory factory) {
        open(player, title, factory, null);
    }

    public static void open(
            ServerPlayer player,
            Component title,
            MenuFactory factory,
            Consumer<NetworkBuffer> extraDataWriter
    ) {
        KineticMenuRuntime.open(
                Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(title, "title"),
                Objects.requireNonNull(factory, "factory"),
                extraDataWriter
        );
    }
}
