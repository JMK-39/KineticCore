package dev.xyat.kineticcore.feature.spawnegg.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@KTNetwork
public final class SpawnEggNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final String MODE_KEY = "DisableEggThrow";
    private static int packetId;
    private static boolean eventRegistered;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KineticCore.MODID, "spawn_egg"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .simpleChannel();

    private SpawnEggNetwork() {
    }

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(SetMode.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SetMode::new)
                .encoder(SetMode::toBytes)
                .consumerMainThread(SetMode::handle)
                .add();

        CHANNEL.messageBuilder(SyncMode.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncMode::new)
                .encoder(SyncMode::toBytes)
                .consumerMainThread(SyncMode::handle)
                .add();

        if (!eventRegistered) {
            eventRegistered = true;
            MinecraftForge.EVENT_BUS.addListener(SpawnEggNetwork::onPlayerLoggedIn);
        }
    }

    public static void sendModeToServer(boolean disabled) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), new SetMode(disabled));
    }

    private static void sendModeToPlayer(ServerPlayer player, boolean disabled) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMode(disabled));
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendModeToPlayer(player, player.getPersistentData().getBoolean(MODE_KEY));
        }
    }

    public record SetMode(boolean disabled) {
        public SetMode(FriendlyByteBuf buffer) {
            this(buffer.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buffer) {
            buffer.writeBoolean(disabled);
        }

        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    player.getPersistentData().putBoolean(MODE_KEY, disabled);
                    sendModeToPlayer(player, disabled);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record SyncMode(boolean disabled) {
        public SyncMode(FriendlyByteBuf buffer) {
            this(buffer.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buffer) {
            buffer.writeBoolean(disabled);
        }

        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> SpawnEggNetworkClient.handleModeSync(disabled)
            ));
            context.setPacketHandled(true);
        }
    }
}
