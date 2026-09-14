package dev.xyat.kineticcore.api.network;

@FunctionalInterface
public interface ServerboundPacketHandler<T> {
    void handle(T message, ServerPacketContext context);
}
