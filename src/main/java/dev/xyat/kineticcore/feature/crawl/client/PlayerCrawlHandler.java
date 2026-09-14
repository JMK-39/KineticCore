package dev.xyat.kineticcore.feature.crawl.client;

import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class PlayerCrawlHandler {
    private static boolean registered;

    private PlayerCrawlHandler() {
    }

    public static void load() {
        if (registered) return;
        registered = true;

        KineticKeyBindings.builder("key.kineticcore.crawl")
                .category("key.categories.movement")
                .context(KineticKeyBindings.Context.IN_GAME)
                .keyboardKey(GLFW.GLFW_KEY_C)
                .registerWhen(() -> KineticFeatureSwitches.isEnabled("player.crawling"))
                .enabledWhen(() -> KineticFeatureSwitches.isEnabled("player.crawling"))
                .onPressed(PlayerCrawlHandler::toggleCrawl)
                .register();
    }

    public static void handleSyncPacket(boolean isCrawling) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (isCrawling) {
            PlayerCrawlStateUtil.startManualCrawling(player);
        } else {
            PlayerCrawlStateUtil.stopManualCrawling(player);
        }
    }

    private static boolean toggleCrawl() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        boolean newState = !PlayerCrawlStateUtil.hasManualCrawlFlag(player);
        PlayerCrawlStateUtil.setCrawling(player, newState);
        PlayerNetwork.sendToServer(new PlayerNetwork.ToggleCrawl(newState));
        return true;
    }
}
