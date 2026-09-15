package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface ClientboundSender<T> {
    void send(ServerPlayer player, T message);

    default void broadcast(T message) {
        throw new UnsupportedOperationException("Broadcast is not supported by this transport");
    }

    default void sendToTrackingAndSelf(Entity entity, T message) {
        throw new UnsupportedOperationException("Tracking-entity delivery is not supported by this transport");
    }
}
