package dev.xyat.kineticcore.api.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class PacketChannel {
    private final NetworkChannel channel;
    private final Map<Class<?>, ServerboundSender<?>> serverboundSenders = new HashMap<>();
    private final Map<Class<?>, ClientboundSender<?>> clientboundSenders = new HashMap<>();

    private PacketChannel(NetworkChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public static PacketChannel create(ResourceLocation id) {
        return new PacketChannel(KineticNetwork.channel(id));
    }

    public static PacketChannel create(ResourceLocation id, String protocolVersion) {
        return new PacketChannel(KineticNetwork.channel(id, protocolVersion));
    }

    public static PacketChannel create(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        return new PacketChannel(KineticNetwork.channel(id, protocolVersion, versionPolicy));
    }

    public ResourceLocation id() {
        return channel.id();
    }

    public synchronized <T> void registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    ) {
        ServerboundSender<T> sender = channel.registerServerbound(messageType, codec, handler);
        serverboundSenders.put(messageType, sender);
    }

    public synchronized <T> void registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        ClientboundSender<T> sender = channel.registerClientbound(messageType, codec, handler);
        clientboundSenders.put(messageType, sender);
    }

    public void sendToServer(Object message) {
        Objects.requireNonNull(message, "message");
        serverboundSender(message.getClass()).send(message);
    }

    public void sendToPlayer(ServerPlayer player, Object message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        clientboundSender(message.getClass()).send(player, message);
    }

    public void broadcast(Object message) {
        Objects.requireNonNull(message, "message");
        clientboundSender(message.getClass()).broadcast(message);
    }

    public void sendToTrackingAndSelf(Entity entity, Object message) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(message, "message");
        clientboundSender(message.getClass()).sendToTrackingAndSelf(entity, message);
    }

    @SuppressWarnings("unchecked")
    private ServerboundSender<Object> serverboundSender(Class<?> type) {
        ServerboundSender<?> sender = serverboundSenders.get(type);
        if (sender == null) {
            throw new IllegalStateException("No serverbound packet registered on " + channel.id() + ": " + type.getName());
        }
        return (ServerboundSender<Object>) sender;
    }

    @SuppressWarnings("unchecked")
    private ClientboundSender<Object> clientboundSender(Class<?> type) {
        ClientboundSender<?> sender = clientboundSenders.get(type);
        if (sender == null) {
            throw new IllegalStateException("No clientbound packet registered on " + channel.id() + ": " + type.getName());
        }
        return (ClientboundSender<Object>) sender;
    }
}
