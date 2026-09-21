package dev.xyat.kineticcore.internal.player;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Internal player-message bridge used by the public Kinetic messaging API. */
public final class KineticPlayerMessageRuntime {
    private KineticPlayerMessageRuntime() {
    }

    /** Sends one server system message. */
    public static void system(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
    }

    /** Sends one normal or overlay client message. */
    public static void display(ServerPlayer player, Component message, boolean overlay) {
        player.displayClientMessage(message, overlay);
    }

    /** Sends title timing followed by the title text packet. */
    public static void title(
            ServerPlayer player,
            Component title,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
