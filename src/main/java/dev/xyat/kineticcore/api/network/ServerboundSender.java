package dev.xyat.kineticcore.api.network;

@FunctionalInterface
public interface ServerboundSender<T> {
    void send(T message);
}
