package dev.xyat.kineticcore.api.network;

/** Callback contract for serverbound packet notifications. */
@FunctionalInterface
public interface ServerboundPacketHandler<T> {
    void handle(T message, ServerPacketContext context);
}
