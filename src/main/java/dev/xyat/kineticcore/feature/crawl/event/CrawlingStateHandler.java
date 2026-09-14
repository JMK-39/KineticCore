package dev.xyat.kineticcore.feature.crawl.event;

import dev.xyat.kineticcore.api.hook.CommonHooks;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.server.event.KineticServerEvents;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

public final class CrawlingStateHandler {
    private static boolean registered;

    private CrawlingStateHandler() {
    }

    public static void load() {
        if (registered) return;

        registered = true;
        CommonHooks.onCrawlPose(player -> {
            if (!PlayerCrawlStateUtil.hasManualCrawlFlag(player)) return false;
            if (PlayerCrawlStateUtil.shouldReleaseToVanilla(player)) {
                PlayerCrawlStateUtil.releaseToVanilla(player);
                return false;
            }
            PlayerCrawlStateUtil.applyManualCrawlPose(player);
            return true;
        });
        KineticServerEvents.onPlayerRespawn((player, endConquered) -> onRespawn(player));
        KineticServerEvents.onPlayerLogin(CrawlingStateHandler::onLogin);
        KineticServerEvents.onPlayerChangedDimension((player, from, to) -> onChangedDimension(player));
        MinecraftForge.EVENT_BUS.addListener(CrawlingStateHandler::onWakeUp);
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

    private static void onWakeUp(PlayerWakeUpEvent event) {
        if (!KineticFeatureSwitches.isEnabled("player.crawling")) return;

        Player player = event.getEntity();
        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerNetwork.sendToPlayer(new PlayerNetwork.SyncCrawl(false), serverPlayer);
        }
    }
}