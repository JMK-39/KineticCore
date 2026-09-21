package dev.xyat.kineticcore.feature.crawl.client;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.flight.KineticFlightClient;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.runtime.KineticFeatures;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.world.entity.player.Player;

public final class PlayerCrawlHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    private PlayerCrawlHandler() {
    }

    public static void load() {
        REGISTRATION.run(() -> KineticKeyBindings.builder("key.kineticcore.crawl")
                .category("key.categories.movement")
                .context(KineticKeyBindings.Context.IN_GAME)
                .keyboard(KineticKeyBindings.Key.C)
                .registerWhen(() -> KineticFeatures.isEnabled("player.crawling"))
                .enabledWhen(() -> KineticFeatures.isEnabled("player.crawling"))
                .onPressed(PlayerCrawlHandler::toggleCrawl)
                .register());
    }

    public static void handleSyncPacket(boolean isCrawling) {
        Player player = KineticClientRuntime.localPlayer();
        if (player == null) return;

        if (isCrawling) {
            PlayerCrawlStateUtil.startManualCrawling(player);
        } else {
            PlayerCrawlStateUtil.stopManualCrawling(player);
        }
    }

    private static boolean toggleCrawl() {
        Player player = KineticClientRuntime.localPlayer();
        if (player == null) return false;

        if (KineticFlightClient.superFlightManeuvering()) {
            if (PlayerCrawlStateUtil.hasManualCrawlFlag(player)) {
                PlayerCrawlStateUtil.clearCrawling(player);
                PlayerNetwork.sendToServer(new PlayerNetwork.ToggleCrawl(false));
            }
            return true;
        }

        boolean newState = !PlayerCrawlStateUtil.hasManualCrawlFlag(player);
        PlayerCrawlStateUtil.setCrawling(player, newState);
        PlayerNetwork.sendToServer(new PlayerNetwork.ToggleCrawl(newState));
        return true;
    }
}
