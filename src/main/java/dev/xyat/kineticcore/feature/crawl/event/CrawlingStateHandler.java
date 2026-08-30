package dev.xyat.kineticcore.feature.crawl.event;

import dev.xyat.kineticcore.MixinPlugin;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

@KTModule
public final class CrawlingStateHandler {
    private static boolean registered;

    private CrawlingStateHandler() {
    }

    public static void load() {
        if (registered) return;

        registered = true;
        MinecraftForge.EVENT_BUS.addListener(CrawlingStateHandler::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(CrawlingStateHandler::onLogin);
        MinecraftForge.EVENT_BUS.addListener(CrawlingStateHandler::onChangedDimension);
        MinecraftForge.EVENT_BUS.addListener(CrawlingStateHandler::onWakeUp);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;

        Player player = event.getEntity();
        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;

        Player player = event.getEntity();
        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;

        Player player = event.getEntity();
        PlayerCrawlStateUtil.clearCrawling(player);
        syncToClient(player);
    }

    private static void onWakeUp(PlayerWakeUpEvent event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;

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