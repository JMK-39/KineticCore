package dev.xyat.kineticcore.api.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Sends one registered server-to-client message type. Call from the server thread.
 *
 * @param <T> message type
 */
@FunctionalInterface
public interface ClientboundSender<T> {
    /** Sends the message to one player. */
    void send(ServerPlayer player, T message);

    /**
     * Sends the message to every connected player.
     *
     * @throws UnsupportedOperationException if this transport has no broadcast; senders created by KineticCore
     *   support it
     */
    default void broadcast(T message) {
        throw new UnsupportedOperationException("Broadcast is not supported by this transport");
    }

    /**
     * Sends the message to every player tracking the entity and, when the entity is a player, to that player too.
     *
     * @throws UnsupportedOperationException if this transport has no tracking delivery; senders created by
     *   KineticCore support it
     */
    default void sendToTrackingAndSelf(Entity entity, T message) {
        throw new UnsupportedOperationException("Tracking-entity delivery is not supported by this transport");
    }
}
