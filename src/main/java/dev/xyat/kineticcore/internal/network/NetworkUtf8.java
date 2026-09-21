package dev.xyat.kineticcore.internal.network;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Strict, wire-compatible UTF-8 conversion for ordinary packet string fields. */
final class NetworkUtf8 {
    private NetworkUtf8() {
    }

    static byte[] encode(String text, int maxChars) {
        requireLimit(maxChars);
        if (text == null) throw new IllegalArgumentException("Network string must not be null");
        if (text.length() > maxChars) throw new IllegalArgumentException("Network string exceeds character limit");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
            if (encoded.remaining() > (long) maxChars * 3L) {
                throw new IllegalArgumentException("Network string exceeds byte limit");
            }
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            return bytes;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("Invalid UTF-16 network string", malformed);
        }
    }

    static String decode(byte[] bytes, int maxChars) {
        requireLimit(maxChars);
        if (bytes == null || bytes.length > (long) maxChars * 3L) {
            throw new IllegalArgumentException("Network string exceeds byte limit");
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (text.length() > maxChars) {
                throw new IllegalArgumentException("Network string exceeds character limit");
            }
            return text;
        } catch (CharacterCodingException malformed) {
            throw new IllegalArgumentException("Invalid UTF-8 network string", malformed);
        }
    }

    private static void requireLimit(int maxChars) {
        if (maxChars < 0) throw new IllegalArgumentException("maxChars must be non-negative");
    }
}
