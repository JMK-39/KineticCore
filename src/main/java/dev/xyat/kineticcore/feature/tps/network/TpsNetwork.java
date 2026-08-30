package dev.xyat.kineticcore.feature.tps.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import dev.xyat.kineticcore.feature.tps.client.TpsRenderer;
import dev.xyat.kineticcore.feature.tps.logic.TpsHudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

@KTNetwork
public final class TpsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KineticCore.MODID, "tps"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .simpleChannel();

    private TpsNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(TpsData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TpsData::new)
                .encoder(TpsData::encode)
                .consumerMainThread(TpsData::handle)
                .add();

        CHANNEL.messageBuilder(SubscriptionData.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SubscriptionData::new)
                .encoder(SubscriptionData::encode)
                .consumerMainThread(SubscriptionData::handle)
                .add();
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendSubscription(boolean enabled) {
        CHANNEL.sendToServer(new SubscriptionData(enabled));
    }

    private static int id() {
        return packetId++;
    }

    public static final class TpsData {
        private final double tps;
        private final double mspt;

        public TpsData(double tps, double mspt) {
            this.tps = tps;
            this.mspt = mspt;
        }

        private TpsData(FriendlyByteBuf buf) {
            this(buf.readDouble(), buf.readDouble());
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeDouble(tps);
            buf.writeDouble(mspt);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                    Dist.CLIENT,
                    () -> () -> TpsRenderer.updateData(tps, mspt)
            ));
            context.setPacketHandled(true);
            return true;
        }
    }

    public static final class SubscriptionData {
        private final boolean enabled;

        public SubscriptionData(boolean enabled) {
            this.enabled = enabled;
        }

        private SubscriptionData(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
        }

        private boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer player = context.getSender();
            if (player != null) {
                context.enqueueWork(() -> TpsHudManager.setEnabled(player, enabled));
            }
            context.setPacketHandled(true);
            return true;
        }
    }
}
