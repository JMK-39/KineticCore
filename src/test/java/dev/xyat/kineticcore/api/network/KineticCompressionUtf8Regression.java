package dev.xyat.kineticcore.api.network;

import java.util.Arrays;

/** Headless regression for strict UTF-8 and existing gzip limits. */
public final class KineticCompressionUtf8Regression {
    public static void main(String[] args) {
        byte[] malformed = KineticCompression.compressBytes(new byte[]{(byte) 0xC3, (byte) 0x28}, 256);
        try {
            KineticCompression.decompressUtf8(malformed, 256);
            throw new AssertionError("invalid UTF-8 must not silently become replacement characters");
        } catch (IllegalArgumentException expected) {
            // Invalid network text was rejected.
        }
        try {
            KineticCompression.compressUtf8("\uD800", 256);
            throw new AssertionError("unpaired UTF-16 surrogate must not be silently replaced while encoding");
        } catch (IllegalArgumentException expected) { }
        String text = "配置与English";
        check(text.equals(KineticCompression.decompressUtf8(KineticCompression.compressUtf8(text, 256), 256)),
                "valid multilingual UTF-8 must round-trip");
        byte[] compressed = KineticCompression.compressBytes(new byte[512], 256);
        try {
            KineticCompression.decompressBytes(compressed, 511);
            throw new AssertionError("decompressed size limit must be enforced");
        } catch (IllegalArgumentException expected) {
            // Size limit still applies.
        }
        check(Arrays.equals(new byte[0], KineticCompression.compressUtf8("", 0)),
                "empty compression must retain existing wire representation");
        System.out.println("PASS: 5 UTF-8/network compression regression cases");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
