package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One versioned network channel. Obtain it from {@link KineticNetwork#channel}, or use the higher-level
 * {@link PacketChannel}.
 *
 * <p>Register every packet type during common setup, in the same order on both sides, or give each a fixed
 * discriminator. Each message class may be registered once per channel. Handlers run on the main game thread;
 * decoding runs on the network thread.
 */
public interface NetworkChannel {
    /** Returns the channel id. */
    ResourceLocation id();

    /**
     * Registers a client-to-server packet with the next free discriminator.
     *
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param handler runs on the server thread with the sending player
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if the message type is already registered on this channel
     */
    <T> ServerboundSender<T> registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    );

    /**
     * Registers a client-to-server packet with a fixed discriminator. Use fixed ids when a registration may be
     * retried after a partial failure, so both sides still agree.
     *
     * @param discriminator stable packet id, {@code 0..2147483646}, unique on this channel
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param handler runs on the server thread with the sending player
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the discriminator is out of range
     * @throws IllegalStateException if the discriminator or message type is already registered on this channel
     */
    <T> ServerboundSender<T> registerServerbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    );

    /**
     * Registers a server-to-client packet with the next free discriminator. Prefer
     * {@link #registerClientboundLazy(Class, NetworkCodec, Supplier)} when the handler touches client-only classes.
     *
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param handler runs on the client thread; never called on a dedicated server
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if the message type is already registered on this channel
     */
    <T> ClientboundSender<T> registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );

    /**
     * Registers a server-to-client packet whose handler is created only on the physical client, so a dedicated
     * server never loads client-only classes.
     *
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param clientHandler supplies the client handler on first use; only called on the physical client
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalStateException if the message type is already registered on this channel
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
    /**
     * Registers a server-to-client packet with a fixed discriminator.
     *
     * @param discriminator stable packet id, {@code 0..2147483646}, unique on this channel
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param handler runs on the client thread; never called on a dedicated server
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the discriminator is out of range
     * @throws IllegalStateException if the discriminator or message type is already registered on this channel
     */
    <T> ClientboundSender<T> registerClientbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );

    /**
     * Registers a lazily resolved server-to-client packet with a fixed discriminator.
     *
     * @param discriminator stable packet id, {@code 0..2147483646}, unique on this channel
     * @param messageType message class; one registration per class and channel
     * @param codec writes and reads the message
     * @param clientHandler supplies the client handler on first use; only called on the physical client
     * @param <T> message type
     * @return the sender for this message type
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if the discriminator is out of range
     * @throws IllegalStateException if the discriminator or message type is already registered on this channel
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
