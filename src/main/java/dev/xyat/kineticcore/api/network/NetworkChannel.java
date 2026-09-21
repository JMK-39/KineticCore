package dev.xyat.kineticcore.api.network;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Public API contract for network channel. */
public interface NetworkChannel {
    ResourceLocation id();

    <T> ServerboundSender<T> registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    );

    /**
     * Registers a serverbound packet with a stable, non-negative protocol ID.
     * Use fixed IDs when retrying a partially failed registration; both peers must
     * use the same IDs regardless of registration attempt order.
     */
    <T> ServerboundSender<T> registerServerbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    );

    <T> ClientboundSender<T> registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );
    /** Registers a clientbound packet with a stable, non-negative protocol ID. */
    <T> ClientboundSender<T> registerClientbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );
}
