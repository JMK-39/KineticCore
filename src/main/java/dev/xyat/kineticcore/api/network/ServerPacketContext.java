package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Information about the player who sent a client-to-server message.
 *
 * @param sender player who sent the message; never {@code null}
 */
public record ServerPacketContext(ServerPlayer sender) {
    /**
     * Creates a context for the given sender.
     *
     * @throws NullPointerException if {@code sender} is {@code null}
     */
    public ServerPacketContext {
        Objects.requireNonNull(sender, "sender");
    }
}
