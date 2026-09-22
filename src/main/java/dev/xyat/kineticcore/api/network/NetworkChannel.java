package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    /**
     * Registers a clientbound packet while deferring creation of the physical-client handler until packet dispatch.
     * Use this overload when the handler references client-only classes so dedicated servers never resolve them
     * during common packet registration.
     */
    default <T> ClientboundSender<T> registerClientboundLazy(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Supplier<Consumer<T>> clientHandler
    ) {
        Objects.requireNonNull(clientHandler, "clientHandler");
        return registerClientbound(messageType, codec, message ->
                KineticPlatform.runOnClient(() -> () -> clientHandler.get().accept(message))
        );
    }
    /** Registers a clientbound packet with a stable, non-negative protocol ID. */
    <T> ClientboundSender<T> registerClientbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );

    /**
     * Registers a fixed-ID clientbound packet with a lazily resolved physical-client handler.
     */
    default <T> ClientboundSender<T> registerClientboundLazy(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Supplier<Consumer<T>> clientHandler
    ) {
        Objects.requireNonNull(clientHandler, "clientHandler");
        return registerClientbound(discriminator, messageType, codec, message ->
                KineticPlatform.runOnClient(() -> () -> clientHandler.get().accept(message))
        );
    }
}
