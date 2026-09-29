package dev.xyat.kineticcore.api.network;

/**
 * Handles one client-to-server message on the server thread. Treat every field as untrusted player input.
 *
 * @param <T> message type
 */
@FunctionalInterface
public interface ServerboundPacketHandler<T> {
    /**
     * Handles a decoded message.
     *
     * @param message decoded message
     * @param context sender information
     */
    void handle(T message, ServerPacketContext context);
}
