package dev.xyat.kineticcore.feature.crawl.network;

import dev.xyat.kineticcore.api.network.PacketChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import dev.xyat.kineticcore.api.network.NetworkCodec;
import dev.xyat.kineticcore.api.network.PacketRegistrations;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerNetwork {
    private static final PacketChannel CHANNEL = PacketChannel.create(
                                                          KineticResourceIds.of("kineticcore", "player_actions"),
                                                          "1",
                                                          NetworkVersionPolicy.EXACT
                                                  );
    private static boolean networkRegistered;
    private static boolean togglePacketRegistered;
    private static boolean syncPacketRegistered;


    private PlayerNetwork() {
    }

    public static synchronized void register() {
        PacketRegistrations.runIndependent(
        () -> {
            if (!togglePacketRegistered) {
                CHANNEL.registerServerbound(0,
                                ToggleCrawl.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.crawling()),
                                buffer -> new ToggleCrawl(buffer.readBoolean())
                        ),
                        (message, context) -> {
                            ServerPlayer player = context.sender();
                            boolean requested = message.crawling();
                            if (requested && KineticSuperFlight.fallFlyingPose(player)) requested = false;
                            PlayerCrawlStateUtil.setCrawling(player, requested);
                            sendToPlayer(new SyncCrawl(PlayerCrawlStateUtil.isCrawling(player)), player);
                        }
                );

                togglePacketRegistered = true;
            }
        },
        () -> {
            if (!syncPacketRegistered) {
                CHANNEL.registerClientboundLazy(1,
                                SyncCrawl.class,
                        NetworkCodec.of(
                                (buffer, message) -> buffer.writeBoolean(message.isCrawling()),
                                buffer -> new SyncCrawl(buffer.readBoolean())
                        ),
                        () -> message -> PlayerNetworkClient.handleSync(message)
                );

                syncPacketRegistered = true;
            }
        },
        () -> networkRegistered = togglePacketRegistered && syncPacketRegistered
        );
    }

    public static void sendToServer(ToggleCrawl message) {
        if (networkRegistered) {
            CHANNEL.sendToServer(message);
        }
    }

    public static void sendToPlayer(SyncCrawl message, ServerPlayer player) {
        if (networkRegistered) {
            CHANNEL.sendToPlayer(player, message);
        }
    }

    public record ToggleCrawl(boolean crawling) {
    }

    public record SyncCrawl(boolean isCrawling) {
    }
}
