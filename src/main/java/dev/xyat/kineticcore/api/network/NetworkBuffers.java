package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.NetworkBufferRuntime;

import java.util.function.Consumer;
import java.util.function.Function;

/** Public API type for network buffers. */
public final class NetworkBuffers {
    private NetworkBuffers() {
    }

    /**
     * Performs the encode API operation.
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
