package dev.xyat.kineticcore.api.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Public API type for packet channel. */
public final class PacketChannel {
    private static final Map<ResourceLocation, PacketChannel> CHANNELS = new HashMap<>();

    private final NetworkChannel channel;
    // Registration is serialized, but sending can happen on other threads.
    private final Map<Class<?>, ServerboundSender<?>> serverboundSenders = new ConcurrentHashMap<>();
    private final Map<Class<?>, ClientboundSender<?>> clientboundSenders = new ConcurrentHashMap<>();

    private PacketChannel(NetworkChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    /**
     * Performs the create API operation.
     */
    public static synchronized PacketChannel create(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        NetworkChannel channel = KineticNetwork.channel(id, protocolVersion, versionPolicy);
        PacketChannel existing = CHANNELS.get(channel.id());
        if (existing != null) {
            return existing;
        }
        PacketChannel created = new PacketChannel(channel);
        CHANNELS.put(channel.id(), created);
        return created;
    }

    /**
     * Returns the id.
     */
    public ResourceLocation id() {
        return channel.id();
    }

    /**
     * Registers serverbound.
     */
    public synchronized <T> void registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    ) {
        ServerboundSender<T> sender = channel.registerServerbound(messageType, codec, handler);
        serverboundSenders.put(messageType, sender);
    }

    /**
     * Registers clientbound.
     */
    public synchronized <T> void registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        ClientboundSender<T> sender = channel.registerClientbound(messageType, codec, handler);
        clientboundSenders.put(messageType, sender);
    }

    /**
     * 以固定协议编号注册服务端数据包。部分注册失败后重试时，客户端和服务端编号必须保持一致。
     */
    public synchronized <T> void registerServerbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    ) {
        ServerboundSender<T> sender = channel.registerServerbound(discriminator, messageType, codec, handler);
        serverboundSenders.put(messageType, sender);
    }

    /** 以固定协议编号注册客户端数据包，禁止与同频道其他数据包使用相同编号。 */
    public synchronized <T> void registerClientbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        ClientboundSender<T> sender = channel.registerClientbound(discriminator, messageType, codec, handler);
        clientboundSenders.put(messageType, sender);
    }

    /**
     * Sends to server.
     */
    public void sendToServer(Object message) {
        Objects.requireNonNull(message, "message");
        serverboundSender(message.getClass()).send(message);
    }

    /**
     * Sends to player.
     */
    public void sendToPlayer(ServerPlayer player, Object message) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(message, "message");
        clientboundSender(message.getClass()).send(player, message);
    }

    /**
     * Performs the broadcast API operation.
     */
    public void broadcast(Object message) {
        Objects.requireNonNull(message, "message");
        clientboundSender(message.getClass()).broadcast(message);
    }

    /**
     * Sends to tracking and self.
     */
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
