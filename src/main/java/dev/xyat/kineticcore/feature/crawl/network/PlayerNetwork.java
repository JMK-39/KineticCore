package dev.xyat.kineticcore.feature.crawl.network;

import dev.xyat.kineticcore.api.runtime.KineticRuntime;
import dev.xyat.kineticcore.api.network.ClientboundSender;
import dev.xyat.kineticcore.api.network.KineticNetwork;
import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.ServerboundSender;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerNetwork {
    private static final NetworkChannel CHANNEL = KineticNetwork.channel(
            new ResourceLocation(KineticRuntime.MOD_ID, "player_actions")
    );

    private static ServerboundSender<ToggleCrawl> toggleCrawlSender;
    private static ClientboundSender<SyncCrawl> syncCrawlSender;

    private PlayerNetwork() {
    }

    public static void register() {
        toggleCrawlSender = CHANNEL.registerServerbound(
                ToggleCrawl.class,
                NetworkCodec.of(
                        (buffer, message) -> buffer.writeBoolean(message.crawling()),
                        buffer -> new ToggleCrawl(buffer.readBoolean())
                ),
                (message, context) -> {
                    ServerPlayer player = context.sender();
                    PlayerCrawlStateUtil.setCrawling(player, message.crawling());
                    sendToPlayer(new SyncCrawl(PlayerCrawlStateUtil.isCrawling(player)), player);
                }
        );

        syncCrawlSender = CHANNEL.registerClientbound(
                SyncCrawl.class,
                NetworkCodec.of(
                        (buffer, message) -> buffer.writeBoolean(message.isCrawling()),
                        buffer -> new SyncCrawl(buffer.readBoolean())
                ),
                PlayerNetworkClient::handleSync
        );
    }

    public static void sendToServer(ToggleCrawl message) {
        if (toggleCrawlSender != null) {
            toggleCrawlSender.send(message);
        }
    }

    public static void sendToPlayer(SyncCrawl message, ServerPlayer player) {
        if (syncCrawlSender != null) {
            syncCrawlSender.send(player, message);
        }
    }

    public record ToggleCrawl(boolean crawling) {
    }

    public record SyncCrawl(boolean isCrawling) {
    }
}
