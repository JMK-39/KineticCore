package dev.xyat.kineticcore.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class NetworkCompressUtil {
    private NetworkCompressUtil() {
    }

    public static byte[] compress(String str) {
        if (str == null || str.isEmpty()) return new byte[0];
        return compressBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] compressBytes(byte[] data) {
        if (data == null || data.length == 0) return new byte[0];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(data);
            gzip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to compress network payload", exception);
        }
    }

    public static String decompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        try {
            return new String(decompressBytes(bytes, Integer.MAX_VALUE), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    public static String decompress(byte[] bytes, int maxDecompressedBytes) {
        if (bytes == null || bytes.length == 0) return "";
        return new String(decompressBytes(bytes, maxDecompressedBytes), StandardCharsets.UTF_8);
    }

    public static byte[] decompressBytes(byte[] bytes, int maxDecompressedBytes) {
        if (bytes == null || bytes.length == 0) return new byte[0];
        if (maxDecompressedBytes < 0) {
            throw new IllegalArgumentException("maxDecompressedBytes must be non-negative");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(8192, Math.max(32, maxDecompressedBytes)))) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                total += read;
                if (total > maxDecompressedBytes) {
                    throw new IllegalArgumentException("Decompressed network payload exceeds limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decompress network payload", exception);
        }
    }
}
