package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Information about the player who sent a client-to-server message. */
public final class ServerPacketContext {
    private final ServerPlayer sender;

    /**
     * Creates a context for the given sender.
     *
     * @param sender player who sent the message
     * @throws NullPointerException if {@code sender} is {@code null}
     */
    public ServerPacketContext(ServerPlayer sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    /** Returns the player who sent the message; never {@code null}. */
    public ServerPlayer sender() {
        return sender;
    }
}
