package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.GzipCompression;

import java.nio.charset.StandardCharsets;

public final class KineticCompression {
    private KineticCompression() {
    }

    public static byte[] compressUtf8(String value) {
        if (value == null || value.isEmpty()) return new byte[0];
        return GzipCompression.compress(value.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] compressUtf8(String value, int maxCompressedBytes) {
        if (value == null || value.isEmpty()) return new byte[0];
        return GzipCompression.compress(value.getBytes(StandardCharsets.UTF_8), maxCompressedBytes);
    }

    public static String decompressUtf8(byte[] value, int maxDecompressedBytes) {
        if (value == null || value.length == 0) return "";
        return new String(GzipCompression.decompress(value, maxDecompressedBytes), StandardCharsets.UTF_8);
    }

    public static byte[] compressBytes(byte[] value) {
        if (value == null || value.length == 0) return new byte[0];
        return GzipCompression.compress(value);
    }

    public static byte[] compressBytes(byte[] value, int maxCompressedBytes) {
        if (value == null || value.length == 0) return new byte[0];
        return GzipCompression.compress(value, maxCompressedBytes);
    }

    public static byte[] decompressBytes(byte[] value, int maxDecompressedBytes) {
        if (value == null || value.length == 0) return new byte[0];
        return GzipCompression.decompress(value, maxDecompressedBytes);
    }
}
