package dev.xyat.kineticcore.internal.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Compile and execute the real ForgeNetworkBuffer UTF/list methods against an isolated wire buffer. */
public final class ForgeNetworkUtf8Regression {
    private static int checks;

    public static void main(String[] args) {
        String text = "中文😀";
        FriendlyByteBuf output = emptyBuffer();
        new ForgeNetworkBuffer(output).writeUtf(text, 16);
        byte[] wire = bytes(output);
        check(wire.length == 1 + text.getBytes(StandardCharsets.UTF_8).length, "compatible length-prefixed wire form");
        check(text.equals(new ForgeNetworkBuffer(buffer(wire)).readUtf(16)), "UTF string roundtrip");
        reject(() -> new ForgeNetworkBuffer(buffer(new byte[]{2, (byte) 0xc3, 0x28})).readUtf(16), "malformed bytes");
        reject(() -> new ForgeNetworkBuffer(buffer(new byte[]{2, 0x61})).readUtf(16), "truncated payload");

        FriendlyByteBuf invalid = emptyBuffer();
        reject(() -> new ForgeNetworkBuffer(invalid).writeUtf("\ud800", 16), "surrogate input");
        check(invalid.readableBytes() == 0, "rejected string does not partially write");

        FriendlyByteBuf listOutput = emptyBuffer();
        reject(() -> new ForgeNetworkBuffer(listOutput).writeStringList(List.of("valid", "\ud800"), 8, 32), "invalid second list entry");
        check(listOutput.readableBytes() == 0, "invalid list does not partially write");

        FriendlyByteBuf validList = emptyBuffer();
        new ForgeNetworkBuffer(validList).writeStringList(List.of("正常", "emoji😀"), 8, 32);
        check(new ForgeNetworkBuffer(buffer(bytes(validList))).readStringList(8, 32)
                .equals(List.of("正常", "emoji😀")), "string list roundtrip");
        System.out.println("PASS: " + checks + " real-network-buffer UTF/list checks");
    }

    private static FriendlyByteBuf emptyBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static FriendlyByteBuf buffer(byte[] bytes) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
    }

    private static byte[] bytes(FriendlyByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }

    private static void reject(Runnable action, String description) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Must reject: " + description);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
