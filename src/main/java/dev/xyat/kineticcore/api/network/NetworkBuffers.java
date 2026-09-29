package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.NetworkBufferRuntime;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Encodes and decodes standalone byte payloads with {@link NetworkBuffer}, for data stored or nested outside a
 * packet.
 */
public final class NetworkBuffers {
    private NetworkBuffers() {
    }

    /**
     * Runs the writer against a fresh buffer and returns the written bytes.
     *
     * @param writer writes the payload
     * @return the encoded bytes
     * @throws NullPointerException if {@code writer} is {@code null}
     */
    public static byte[] encode(Consumer<NetworkBuffer> writer) {
        return NetworkBufferRuntime.encode(writer);
    }

    /**
     * Decodes one complete payload. Rejects unread trailing bytes rather than
     * silently accepting malformed or mismatched protocol data.
     */
    public static <T> T decode(byte[] bytes, Function<NetworkBuffer, T> reader) {
        return NetworkBufferRuntime.decode(bytes, reader);
    }
}
