package dev.xyat.kineticcore.feature.crawl.client;

import dev.xyat.kineticcore.MixinPlugin;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.feature.crawl.network.PlayerNetwork;
import dev.xyat.kineticcore.feature.crawl.util.PlayerCrawlStateUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@KTClientModule
public final class PlayerCrawlHandler {
    public static final KeyMapping CRAWL_KEY = new KeyMapping(
            "key.kineticcore.crawl",
            GLFW.GLFW_KEY_C,
            "key.categories.movement"
    );

    private static boolean registered;

    private PlayerCrawlHandler() {
    }

    public static void load() {
        if (registered) return;

        registered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PlayerCrawlHandler::onRegisterKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, PlayerCrawlHandler::onKeyInput);
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

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;
        event.register(CRAWL_KEY);
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (!MixinPlugin.isFeatureEnabled("feature.crawl.PlayerCrawlPoseMixin")) return;
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) return;

        while (CRAWL_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean newState = !PlayerCrawlStateUtil.hasManualCrawlFlag(player);

            PlayerCrawlStateUtil.setCrawling(player, newState);
            PlayerNetwork.sendToServer(new PlayerNetwork.ToggleCrawl(newState));
        }
    }
}