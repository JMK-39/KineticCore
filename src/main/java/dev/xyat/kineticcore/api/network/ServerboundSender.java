package dev.xyat.kineticcore.api.network;

/** Sending contract for serverbound messages. */
@FunctionalInterface
public interface ServerboundSender<T> {
    void send(T message);
}
