package dev.xyat.kineticcore.feature.crawl.network;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.api.KTNetworkProtocol;
import dev.xyat.kineticcore.bootstrap.annotation.KTNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
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
public class PlayerNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KineticCore.MODID, "player_actions"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .serverAcceptedVersions(KTNetworkProtocol::acceptsAnyVersion)
            .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(ToggleCrawl.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleCrawl::new)
                .encoder(ToggleCrawl::toBytes)
                .consumerMainThread(ToggleCrawl::handle)
                .add();

        CHANNEL.messageBuilder(SyncCrawl.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncCrawl::new)
                .encoder(SyncCrawl::toBytes)
                .consumerMainThread(SyncCrawl::handle)
                .add();
    }

    public record ToggleCrawl(boolean crawling) {
        public ToggleCrawl(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(crawling);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                PlayerCrawlStateUtil.setCrawling(player, crawling);
                sendToPlayer(new SyncCrawl(PlayerCrawlStateUtil.isCrawling(player)), player);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public record SyncCrawl(boolean isCrawling) {
        public SyncCrawl(FriendlyByteBuf buf) {
            this(buf.readBoolean());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(isCrawling);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerNetworkClient.handleSync(this)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static void sendToServer(Object msg) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
