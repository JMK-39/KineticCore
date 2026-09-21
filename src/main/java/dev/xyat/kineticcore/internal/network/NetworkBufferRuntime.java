package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.network.NetworkBuffer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Objects;

public final class NetworkBufferRuntime {
    private NetworkBufferRuntime() {
    }

    public static NetworkBuffer wrap(FriendlyByteBuf buffer) {
        return new ForgeNetworkBuffer(java.util.Objects.requireNonNull(buffer, "buffer"));
    }

    public static byte[] encode(Consumer<NetworkBuffer> writer) {
        Objects.requireNonNull(writer, "writer");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(new ForgeNetworkBuffer(buffer));
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    public static <T> T decode(byte[] bytes, Function<NetworkBuffer, T> reader) {
        Objects.requireNonNull(reader, "reader");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes == null ? new byte[0] : bytes));
        try {
            T value = reader.apply(new ForgeNetworkBuffer(buffer));
            // A complete standalone packet must not silently accept an unknown suffix.
            if (buffer.readableBytes() != 0) {
                throw new IllegalArgumentException("Unexpected trailing network payload bytes");
            }
            return value;
        } finally {
            buffer.release();
        }
    }
}
