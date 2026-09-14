package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class ServerPacketContext {
    private final ServerPlayer sender;

    public ServerPacketContext(ServerPlayer sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    public ServerPlayer sender() {
        return sender;
    }
}
