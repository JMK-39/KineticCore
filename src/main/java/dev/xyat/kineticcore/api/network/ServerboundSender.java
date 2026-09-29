package dev.xyat.kineticcore.api.network;

/**
 * Sends one registered client-to-server message type. Call from the client thread while connected.
 *
 * @param <T> message type
 */
@FunctionalInterface
public interface ServerboundSender<T> {
    /** Sends the message to the server. */
    void send(T message);
}
