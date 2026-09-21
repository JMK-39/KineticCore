package dev.xyat.kineticcore.internal.network;

import java.nio.charset.StandardCharsets;

/** The network text codec must preserve valid strings and reject invalid Unicode on both ends. */
public final class NetworkUtf8Regression {
    private static int checks;

    public static void main(String[] args) {
        byte[] normal = NetworkUtf8.encode("中文 + hello", 32);
        check("中文 + hello".equals(NetworkUtf8.decode(normal, 32)), "multilingual round-trip");
        byte[] emoji = NetworkUtf8.encode("A😀B", 4);
        check("A😀B".equals(NetworkUtf8.decode(emoji, 4)), "supplementary codepoint round-trip");
        reject(() -> NetworkUtf8.encode("\uD800", 4), "unpaired high surrogate");
        reject(() -> NetworkUtf8.encode("\uDC00", 4), "unpaired low surrogate");
        reject(() -> NetworkUtf8.decode(new byte[]{(byte)0xC3, (byte)0x28}, 8), "bad continuation byte");
        reject(() -> NetworkUtf8.decode(new byte[]{(byte)0xC0, (byte)0xAF}, 8), "overlong UTF-8");
        reject(() -> NetworkUtf8.decode(new byte[]{(byte)0xED, (byte)0xA0, (byte)0x80}, 8), "encoded surrogate");
        reject(() -> NetworkUtf8.decode("hello".getBytes(StandardCharsets.UTF_8), 4), "decoded character limit");
        reject(() -> NetworkUtf8.encode("hello", 4), "encoded character limit");
        reject(() -> NetworkUtf8.encode("x", -1), "negative encode limit");
        reject(() -> NetworkUtf8.decode(new byte[]{'x'}, -1), "negative decode limit");
        check(NetworkUtf8.encode("", 0).length == 0, "zero-length UTF limit");
        System.out.println("PASS: " + checks + " strict network UTF-8 checks");
    }

    private static void reject(Runnable action, String description) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Must reject " + description);
    }
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }
}
