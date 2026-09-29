package dev.xyat.kineticcore.api.network;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Writes and reads one message type. The decoder must read exactly what the encoder wrote, in the same order.
 *
 * @param <T> message type
 */
public interface NetworkCodec<T> {
    /** Writes the message; runs on the sending thread. */
    void encode(NetworkBuffer buffer, T message);

    /** Reads a message; runs on the network thread, so it must not touch game state. */
    T decode(NetworkBuffer buffer);

    /**
     * Creates a codec from two functions, typically method references such as {@code MyMessage::write} and
     * {@code MyMessage::read}.
     *
     * @param encoder writes a message
     * @param decoder reads a message
     * @param <T> message type
     * @return the codec
     * @throws NullPointerException if an argument is {@code null}
     */
    static <T> NetworkCodec<T> of(
            BiConsumer<NetworkBuffer, T> encoder,
            Function<NetworkBuffer, T> decoder
    ) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(decoder, "decoder");
        return new NetworkCodec<>() {
            @Override
            public void encode(NetworkBuffer buffer, T message) {
                encoder.accept(buffer, message);
            }

            @Override
            public T decode(NetworkBuffer buffer) {
                return decoder.apply(buffer);
            }
        };
    }
}
