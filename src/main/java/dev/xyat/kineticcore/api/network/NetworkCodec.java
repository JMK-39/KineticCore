package dev.xyat.kineticcore.api.network;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface NetworkCodec<T> {
    void encode(NetworkBuffer buffer, T message);

    T decode(NetworkBuffer buffer);

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
