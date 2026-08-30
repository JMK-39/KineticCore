package dev.xyat.kineticcore.feature.pvp.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
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
public class PvpNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KineticCore.MODID, "pvp"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .simpleChannel();

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(S2CPvpStatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CPvpStatePacket::new)
                .encoder(S2CPvpStatePacket::toBytes)
                .consumerMainThread(S2CPvpStatePacket::handle)
                .add();
    }

    public static void sendState(ServerPlayer player, boolean enabled) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPvpStatePacket(enabled));
    }

    public static class S2CPvpStatePacket {
        private final boolean enabled;

        public S2CPvpStatePacket(boolean enabled) {
            this.enabled = enabled;
        }

        public S2CPvpStatePacket(FriendlyByteBuf buf) {
            this.enabled = buf.readBoolean();
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(this.enabled);
        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PvpNetworkHandlerClient.handleState(enabled)));
            context.setPacketHandled(true);
            return true;
        }
    }
}
