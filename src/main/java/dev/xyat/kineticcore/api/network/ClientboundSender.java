package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ClientboundSender<T> {
    void send(ServerPlayer player, T message);
}
