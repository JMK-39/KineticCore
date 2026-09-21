package dev.xyat.kineticcore.internal.config;

import dev.xyat.kineticcore.api.network.NetworkProtocolLimits;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** The sender's explicit array bound must agree with compression and decoder bounds. */
public final class ServerConfigPacketLimitRegression {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/xyat/kineticcore/internal/config/ServerConfigNetwork.java"), StandardCharsets.UTF_8);
        check(NetworkProtocolLimits.DEFAULT.maxByteArrayBytes() == 1024 * 1024,
                "test requires the default array ceiling to remain lower than the config packet ceiling");
        check(source.contains("MAX_COMPRESSED_BYTES = 2 * 1024 * 1024"), "config advertised ceiling");
        check(count(source, "buffer.writeByteArray(packet.payload(), MAX_COMPRESSED_BYTES)") == 2,
                "both save and sync encoders must explicitly use config ceiling");
        check(count(source, "buffer.readByteArray(MAX_COMPRESSED_BYTES)") == 2,
                "both save and sync decoders must use matching ceiling");
        System.out.println("PASS: 4 server-config packet limit checks");
    }

    private static int count(String text, String part) {
        int matches = 0;
        int offset = 0;
        while ((offset = text.indexOf(part, offset)) != -1) { matches++; offset += part.length(); }
        return matches;
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
