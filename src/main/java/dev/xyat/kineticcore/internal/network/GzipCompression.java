package dev.xyat.kineticcore.internal.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GzipCompression {
    private GzipCompression() {
    }

    public static byte[] compress(byte[] data) {
        return compress(data, Integer.MAX_VALUE);
    }

    public static byte[] compress(byte[] data, int maxCompressedBytes) {
        if (maxCompressedBytes < 0) {
            throw new IllegalArgumentException("maxCompressedBytes must be non-negative");
        }
        try (LimitedByteArrayOutputStream output = new LimitedByteArrayOutputStream(maxCompressedBytes);
             GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(data);
            gzip.finish();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to compress network payload", exception);
        }
    }

    public static byte[] decompress(byte[] data, int maxDecompressedBytes) {
        if (maxDecompressedBytes < 0) {
            throw new IllegalArgumentException("maxDecompressedBytes must be non-negative");
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(8192, Math.max(32, maxDecompressedBytes)))) {
            byte[] chunk = new byte[8192];
            int total = 0;
            int read;
            while ((read = gzip.read(chunk)) != -1) {
                total += read;
                if (total > maxDecompressedBytes) {
                    throw new IllegalArgumentException("Decompressed network payload exceeds limit");
                }
                output.write(chunk, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to decompress network payload", exception);
        }
    }

    private static final class LimitedByteArrayOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        private final int limit;
        private int count;

        private LimitedByteArrayOutputStream(int limit) {
            this.limit = limit;
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            delegate.write(value);
            count++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ensureCapacity(length);
            delegate.write(bytes, offset, length);
            count += length;
        }

        private void ensureCapacity(int additional) throws IOException {
            if (additional < 0 || count > limit - additional) {
                throw new IOException("Compressed network payload exceeds limit");
            }
        }

        private byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }
}
