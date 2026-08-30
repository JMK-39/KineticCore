package dev.xyat.kineticcore.feature.crawl.network;

import dev.xyat.kineticcore.feature.crawl.client.PlayerCrawlHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PlayerNetworkClient {
    private PlayerNetworkClient() {
    }

    public static void handleSync(PlayerNetwork.SyncCrawl packet) {
        PlayerCrawlHandler.handleSyncPacket(packet.isCrawling());
    }
}