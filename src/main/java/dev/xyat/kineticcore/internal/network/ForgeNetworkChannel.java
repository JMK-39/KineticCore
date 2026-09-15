package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.ServerPacketContext;
import dev.xyat.kineticcore.api.network.ServerboundPacketHandler;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class ForgeNetworkChannel implements NetworkChannel {
    private final ResourceLocation id;
    private final SimpleChannel channel;
    private final Set<Class<?>> serverboundTypes = new HashSet<>();
    private final Set<Class<?>> clientboundTypes = new HashSet<>();
    private int nextPacketId;

    public ForgeNetworkChannel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        this.id = Objects.requireNonNull(id, "id");
        String version = Objects.requireNonNull(protocolVersion, "protocolVersion").trim();
        NetworkVersionPolicy policy = Objects.requireNonNull(versionPolicy, "versionPolicy");
        if (version.isEmpty()) {
            throw new IllegalArgumentException("protocolVersion cannot be blank");
        }
        this.channel = NetworkRegistry.ChannelBuilder
                .named(id)
                .networkProtocolVersion(() -> version)
                .clientAcceptedVersions(remoteVersion -> acceptsVersion(policy, version, remoteVersion))
                .serverAcceptedVersions(remoteVersion -> acceptsVersion(policy, version, remoteVersion))
                .simpleChannel();
    }

    private static boolean acceptsVersion(
            NetworkVersionPolicy policy,
            String localVersion,
            String remoteVersion
    ) {
        return policy == NetworkVersionPolicy.ANY || localVersion.equals(remoteVersion);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public synchronized <T> ServerboundSender<T> registerServerbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    ) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(handler, "handler");
        if (!serverboundTypes.add(messageType)) {
            throw new IllegalStateException(
                    "Serverbound packet type is already registered on " + id + ": " + messageType.getName()
            );
        }

        channel.messageBuilder(messageType, nextPacketId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> codec.encode(new ForgeNetworkBuffer(buffer), message))
                .decoder(buffer -> codec.decode(new ForgeNetworkBuffer(buffer)))
                .consumerMainThread((message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    if (context.getSender() != null) {
                        handler.handle(message, new ServerPacketContext(context.getSender()));
                    }
                    context.setPacketHandled(true);
                })
                .add();

        return message -> channel.send(PacketDistributor.SERVER.noArg(), message);
    }

    @Override
    public synchronized <T> ClientboundSender<T> registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(handler, "handler");
        if (!clientboundTypes.add(messageType)) {
            throw new IllegalStateException(
                    "Clientbound packet type is already registered on " + id + ": " + messageType.getName()
            );
        }

        channel.messageBuilder(messageType, nextPacketId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> codec.encode(new ForgeNetworkBuffer(buffer), message))
                .decoder(buffer -> codec.decode(new ForgeNetworkBuffer(buffer)))
                .consumerMainThread((message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(message));
                    context.setPacketHandled(true);
                })
                .add();

        return new ClientboundSender<>() {
            @Override
            public void send(net.minecraft.server.level.ServerPlayer player, T message) {
                channel.send(PacketDistributor.PLAYER.with(() -> player), message);
            }

            @Override
            public void broadcast(T message) {
                channel.send(PacketDistributor.ALL.noArg(), message);
            }

            @Override
            public void sendToTrackingAndSelf(net.minecraft.world.entity.Entity entity, T message) {
                channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
            }
        };
    }
}
