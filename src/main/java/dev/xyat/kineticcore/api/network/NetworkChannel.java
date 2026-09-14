package dev.xyat.kineticcore.api.network;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public interface NetworkChannel {
    ResourceLocation id();

    <T> ServerboundSender<T> registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    );

    <T> ClientboundSender<T> registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    );
}
