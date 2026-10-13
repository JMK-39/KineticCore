package dev.xyat.kineticcore.feature.itemcreator;

import dev.xyat.kineticcore.api.network.NetworkBuffers;
import dev.xyat.kineticcore.api.network.NetworkBuffer;
import java.lang.reflect.InvocationTargetException;

public final class ItemCreatorPacketRegression {
    public static void main(String[] args) throws Exception {
        Class<?> codec;
        try { codec = Class.forName("dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Missing structured item creator protocol", e); }
        var read = codec.getMethod("readRequest", NetworkBuffer.class);
        byte[] excessive = NetworkBuffers.encode(b -> {
            b.writeVarInt(41); b.writeBoolean(true); b.writeUtf("minecraft:zombie", 256); b.writeVarInt(129);
        });
        reject(excessive, read, "oversized attributes");
        byte[] invalidEffect = NetworkBuffers.encode(b -> {
            b.writeVarInt(42); b.writeBoolean(false); b.writeVarInt(0);
            b.writeVarInt(1); b.writeUtf("minecraft:speed", 256); b.writeVarInt(256); b.writeInt(20); b.writeBoolean(true); b.writeBoolean(true);
        });
        reject(invalidEffect, read, "amplifier 256");
        byte[] truncated = {1}; reject(truncated, read, "truncated request");
        var named = new dev.xyat.kineticcore.api.itemcreator.SpawnEggTemplate("minecraft:zombie", java.util.List.of(), java.util.List.of(), "测试生物");
        var request = new dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol.Request(43, named, null, java.util.List.of());
        var bytes = NetworkBuffers.encode(b -> dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol.writeRequest(b, request));
        if (!NetworkBuffers.decode(bytes, dev.xyat.kineticcore.internal.itemcreator.ItemCreatorProtocol::readRequest).equals(request)) throw new AssertionError("Named request did not round trip");
        if (!dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData.decode(dev.xyat.kineticcore.api.itemcreator.ItemCreatorStackData.encode(named)).equals(named)) throw new AssertionError("Named item data did not round trip");
        try { new dev.xyat.kineticcore.api.itemcreator.SpawnEggTemplate("minecraft:zombie", java.util.List.of(), java.util.List.of(), "x".repeat(129)); throw new AssertionError("Unbounded name"); } catch (IllegalArgumentException expected) { }
        System.out.println("PASS: item creator bounded protocol rejects oversized, invalid and truncated requests");
    }
    private static void reject(byte[] bytes, java.lang.reflect.Method read, String label) {
        try {
            NetworkBuffers.decode(bytes, b -> {
                try { return read.invoke(null, b); }
                catch (InvocationTargetException e) {
                    if (e.getCause() instanceof RuntimeException r) throw r;
                    throw new IllegalStateException(e);
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            });
        } catch (IllegalArgumentException | IndexOutOfBoundsException expected) { return; }
        throw new AssertionError("Accepted " + label);
    }
}
