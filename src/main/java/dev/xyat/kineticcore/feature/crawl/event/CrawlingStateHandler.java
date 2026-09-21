package dev.xyat.kineticcore.feature.crawl.event;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.flight.KineticSuperFlight;
import dev.xyat.kineticcore.api.event.KineticEventPriority;
import dev.xyat.kineticcore.api.hook.CommonHooks;
import dev.xyat.kineticcore.api.player.event.KineticPlayerEvents;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CrawlingStateHandler {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    private CrawlingStateHandler() {
    }

    public static void load() {
        REGISTRATION.run(
                () -> CommonHooks.onPlayerPoseUpdate(player -> {
                    if (KineticSuperFlight.fallFlyingPose(player)) {
                        PlayerCrawlStateUtil.clearCrawling(player);
                        return false;
                    }
                    if (!PlayerCrawlStateUtil.hasManualCrawlFlag(player)) return false;
                    if (PlayerCrawlStateUtil.shouldReleaseToVanilla(player)) {
                        PlayerCrawlStateUtil.releaseToVanilla(player);
                        return false;
                    }
                    PlayerCrawlStateUtil.applyManualCrawlPose(player);
                    return true;
                }),
                () -> KineticServerEvents.onPlayerRespawn(KineticEventPriority.NORMAL, (player, endConquered) -> onRespawn(player)),
                () -> KineticServerEvents.onPlayerLogin(KineticEventPriority.NORMAL, CrawlingStateHandler::onLogin),
                () -> KineticServerEvents.onPlayerChangedDimension(KineticEventPriority.NORMAL, (player, from, to) -> onChangedDimension(player)),
                () -> KineticPlayerEvents.onWakeUp(KineticEventPriority.NORMAL, CrawlingStateHandler::onWakeUp)
        );
    }

    private static void onRespawn(ServerPlayer player) {
        if (!KineticFeatureSwitches.isEnabled("player.crawling")) return;

        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onLogin(ServerPlayer player) {
        if (!KineticFeatureSwitches.isEnabled("player.crawling")) return;

        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onChangedDimension(ServerPlayer player) {
        if (!KineticFeatureSwitches.isEnabled("player.crawling")) return;

        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onWakeUp(Player player) {
        if (!KineticFeatureSwitches.isEnabled("player.crawling")) return;

        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerNetwork.sendToPlayer(new PlayerNetwork.SyncCrawl(false), serverPlayer);
        }
    }
}