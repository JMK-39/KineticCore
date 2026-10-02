package dev.xyat.kineticcore.internal.network;

//? if neoforge {
/*import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.ServerPacketContext;
import dev.xyat.kineticcore.api.network.ServerboundPacketHandler;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

// NetworkChannel on NeoForge's payload system. Every message type registered on a channel becomes its own payload
// type "<channel namespace>:<channel path>/<discriminator>". NeoForge only accepts payloads while
// RegisterPayloadHandlersEvent fires, after mod loading has completed, so registrations wait here until then.
public final class NeoForgeNetworkChannel implements NetworkChannel {
    private static final List<Consumer<RegisterPayloadHandlersEvent>> PENDING = new ArrayList<>();
    private static boolean listening;
    private static boolean registrationClosed;

    private final ResourceLocation id;
    private final String protocolVersion;
    private final NetworkVersionPolicy versionPolicy;
    private final Set<Class<?>> registeredTypes = new HashSet<>();
    private final Set<Integer> registeredPacketIds = new HashSet<>();
    private int nextPacketId;

    public NeoForgeNetworkChannel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion").trim();
        this.versionPolicy = Objects.requireNonNull(versionPolicy, "versionPolicy");
        if (this.protocolVersion.isEmpty()) {
            throw new IllegalArgumentException("protocolVersion cannot be blank");
        }
        listen();
    }

    private static synchronized void listen() {
        if (listening) return;
        // KineticCore's own mod bus: channels may be created outside any mod's construction.
        ModList.get().getModContainerById("kineticcore").orElseThrow().getEventBus()
                .addListener(NeoForgeNetworkChannel::onRegisterPayloads);
        listening = true;
    }

    private static synchronized void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        registrationClosed = true;
        for (Consumer<RegisterPayloadHandlersEvent> registration : PENDING) registration.accept(event);
        PENDING.clear();
    }

    private static synchronized void enqueue(ResourceLocation payloadId, Consumer<RegisterPayloadHandlersEvent> registration) {
        if (registrationClosed) {
            throw new IllegalStateException("Packet " + payloadId + " must be registered before mod loading completes;"
                    + " NeoForge closes payload registration after that");
        }
        PENDING.add(registration);
    }

    private PayloadRegistrar registrar(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(protocolVersion);
        // ANY: peers without this channel, or with another version of it, may still connect.
        return versionPolicy == NetworkVersionPolicy.ANY ? registrar.optional() : registrar;
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
        return registerServerbound(nextPacketId, messageType, codec, handler);
    }

    @Override
    public synchronized <T> ServerboundSender<T> registerServerbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            ServerboundPacketHandler<T> handler
    ) {
        Objects.requireNonNull(handler, "handler");
        CustomPacketPayload.Type<Payload<T>> type = claim(discriminator, messageType, codec);
        StreamCodec<RegistryFriendlyByteBuf, Payload<T>> streamCodec = streamCodec(type, codec);
        enqueue(type.id(), event -> registrar(event).playToServer(type, streamCodec, (payload, context) -> {
            if (context.player() instanceof ServerPlayer sender) {
                handler.handle(payload.message(), new ServerPacketContext(sender));
            }
        }));
        return message -> PacketDistributor.sendToServer(new Payload<>(type, message));
    }

    @Override
    public synchronized <T> ClientboundSender<T> registerClientbound(
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        return registerClientbound(nextPacketId, messageType, codec, handler);
    }

    @Override
    public synchronized <T> ClientboundSender<T> registerClientbound(
            int discriminator,
            Class<T> messageType,
            NetworkCodec<T> codec,
            Consumer<T> handler
    ) {
        Objects.requireNonNull(handler, "handler");
        CustomPacketPayload.Type<Payload<T>> type = claim(discriminator, messageType, codec);
        StreamCodec<RegistryFriendlyByteBuf, Payload<T>> streamCodec = streamCodec(type, codec);
        enqueue(type.id(), event -> registrar(event).playToClient(type, streamCodec, (payload, context) ->
                KineticEnvironmentRuntime.runOnClient(() -> () -> handler.accept(payload.message()))));
        return new ClientboundSender<>() {
            @Override
            public void send(ServerPlayer player, T message) {
                PacketDistributor.sendToPlayer(player, new Payload<>(type, message));
            }

            @Override
            public void broadcast(T message) {
                PacketDistributor.sendToAllPlayers(new Payload<>(type, message));
            }

            @Override
            public void sendToTrackingAndSelf(Entity entity, T message) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new Payload<>(type, message));
            }
        };
    }

    private <T> CustomPacketPayload.Type<Payload<T>> claim(int discriminator, Class<T> messageType, NetworkCodec<T> codec) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(codec, "codec");
        if (registeredTypes.contains(messageType)) {
            throw new IllegalStateException(
                    "Packet type is already registered on " + id + ": " + messageType.getName()
            );
        }
        if (discriminator < 0 || discriminator == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Packet discriminator must be within 0..2147483646: " + discriminator);
        }
        if (registeredPacketIds.contains(discriminator)) {
            throw new IllegalStateException("Packet discriminator is already registered on " + id + ": " + discriminator);
        }
        registeredTypes.add(messageType);
        registeredPacketIds.add(discriminator);
        nextPacketId = Math.max(nextPacketId, discriminator + 1);
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "/" + discriminator));
    }

    private static <T> StreamCodec<RegistryFriendlyByteBuf, Payload<T>> streamCodec(
            CustomPacketPayload.Type<Payload<T>> type,
            NetworkCodec<T> codec
    ) {
        return StreamCodec.of(
                (buffer, payload) -> codec.encode(new ForgeNetworkBuffer(buffer), payload.message()),
                buffer -> new Payload<>(type, codec.decode(new ForgeNetworkBuffer(buffer)))
        );
    }

    private record Payload<T>(Type<Payload<T>> type, T message) implements CustomPacketPayload {
    }
}
*///?}
