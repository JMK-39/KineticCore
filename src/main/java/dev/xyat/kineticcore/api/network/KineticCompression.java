package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.GzipCompression;

import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Compression helpers with explicit caller-defined size limits.
 */
public final class KineticCompression {
    /** Standard decompressed payload ceiling preserved from the original Kinetic transport defaults. */
    public static final int DEFAULT_MAX_DECOMPRESSED_BYTES = 8 * 1024 * 1024;

    private KineticCompression() {
    }

    /** Compresses UTF-8 text with an explicit compressed-size limit. */
    public static byte[] compressUtf8(String value, int maxCompressedBytes) {
        return compressUtf8(value, maxCompressedBytes, Integer.MAX_VALUE);
    }

    /**
     * Compresses UTF-8 text subject to both wire-size and decoded-byte limits.
     * Use the receiver's decompression ceiling as {@code maxUtf8Bytes} so an
     * extremely compressible snapshot cannot be sent only to fail on receipt.
     * The existing two-argument overload retains its original behavior.
     */
    public static byte[] compressUtf8(String value, int maxCompressedBytes, int maxUtf8Bytes) {
        requireNonNegative(maxCompressedBytes, "maxCompressedBytes");
        requireNonNegative(maxUtf8Bytes, "maxUtf8Bytes");
        if (value == null || value.isEmpty()) return new byte[0];
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            if (encoded.remaining() > maxUtf8Bytes) {
                throw new IllegalArgumentException("Uncompressed network text exceeds length limit");
            }
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return GzipCompression.compress(bytes, maxCompressedBytes);
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("Invalid UTF-8 network text", malformed);
        }
    }

    /** Decompresses UTF-8 text and rejects payloads that exceed the caller-defined decompressed-size limit. */
    public static String decompressUtf8(byte[] value, int maxDecompressedBytes) {
        requireNonNegative(maxDecompressedBytes, "maxDecompressedBytes");
        if (value == null || value.length == 0) return "";
        byte[] decoded = GzipCompression.decompress(value, maxDecompressedBytes);
        try {
            // Malformed payloads must not silently become a different configuration string.
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(decoded)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("Invalid UTF-8 network payload", malformed);
        }
    }

    /** Compresses bytes with an explicit compressed-size limit. */
    public static byte[] compressBytes(byte[] value, int maxCompressedBytes) {
        requireNonNegative(maxCompressedBytes, "maxCompressedBytes");
        if (value == null || value.length == 0) return new byte[0];
        return GzipCompression.compress(value, maxCompressedBytes);
    }

    /** Decompresses binary data and rejects payloads that exceed the caller-defined decompressed-size limit. */
    public static byte[] decompressBytes(byte[] value, int maxDecompressedBytes) {
        requireNonNegative(maxDecompressedBytes, "maxDecompressedBytes");
        if (value == null || value.length == 0) return new byte[0];
        return GzipCompression.decompress(value, maxDecompressedBytes);
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }
}
