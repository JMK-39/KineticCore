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

/** Opens container menus for players from server code. Call these methods on the server thread. */
public final class KineticMenus {
    /** Creates the server-side menu instance for one player. */
    @FunctionalInterface
    public interface MenuFactory {
        /**
         * Creates the server-side menu.
         *
         * @param containerId container id to pass to the menu constructor
         * @param inventory the player's inventory
         * @param player the player the menu is opened for
         * @return the new menu
         */
        AbstractContainerMenu create(int containerId, Inventory inventory, Player player);
    }

    private KineticMenus() {
    }


    /** Opens a menu that does not require extra opening payload data. */
    public static void open(
            ServerPlayer player,
            Component title,
            MenuFactory factory
    ) {
        open(player, title, factory, null);
    }

    /**
     * Opens a menu and sends extra data that the client-side {@code KineticMenuTypes.Factory} reads back.
     *
     * @param player player to open the menu for
     * @param title menu title shown on the client
     * @param factory creates the server-side menu
     * @param extraDataWriter writes the opening payload, or {@code null} to send none
     * @throws NullPointerException if {@code player}, {@code title} or {@code factory} is {@code null}
     */
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
