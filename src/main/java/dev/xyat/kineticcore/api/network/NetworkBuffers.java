package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.NetworkBufferRuntime;

import java.util.function.Consumer;
import java.util.function.Function;

public final class NetworkBuffers {
    private NetworkBuffers() {
    }

    public static byte[] encode(Consumer<NetworkBuffer> writer) {
        return NetworkBufferRuntime.encode(writer);
    }

    public static <T> T decode(byte[] bytes, Function<NetworkBuffer, T> reader) {
        return NetworkBufferRuntime.decode(bytes, reader);
    }
}
