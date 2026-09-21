package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Public API type for server packet context. */
public final class ServerPacketContext {
    private final ServerPlayer sender;

    /**
     * Creates a new server packet context instance.
     */
    public ServerPacketContext(ServerPlayer sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    /**
     * Sends er.
     */
    public ServerPlayer sender() {
        return sender;
    }
}
