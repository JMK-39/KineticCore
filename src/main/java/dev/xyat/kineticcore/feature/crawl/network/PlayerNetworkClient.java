package dev.xyat.kineticcore.feature.crawl.network;

import dev.xyat.kineticcore.feature.crawl.client.PlayerCrawlHandler;

public final class PlayerNetworkClient {
    private PlayerNetworkClient() {
    }

    public static void handleSync(PlayerNetwork.SyncCrawl packet) {
        PlayerCrawlHandler.handleSyncPacket(packet.isCrawling());
    }
}