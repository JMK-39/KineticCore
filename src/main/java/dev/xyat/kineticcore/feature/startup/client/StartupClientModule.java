package dev.xyat.kineticcore.feature.startup.client;

import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.feature.startup.config.StartupConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.management.ManagementFactory;

@KTClientModule
public final class StartupClientModule {
    private static long totalStartupTime = -1L;
    private static long firstTitleScreenRenderTime = -1L;
    private static boolean calculated;

    private StartupClientModule() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StartupClientModule());
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Pre event) {
        if (calculated) return;
        calculated = true;
        long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        totalStartupTime = System.currentTimeMillis() - jvmStartTime;
    }

    @SubscribeEvent
    public void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        int currentY = StartupConfig.anchorY();
        int spacing = 2;

        if (StartupConfig.showLoginInfo()) {
            User user = minecraft.getUser();
            event.getGuiGraphics().drawString(
                    minecraft.font,
                    Component.translatable("msg.kineticcore.startup.account_id", Component.literal(user.getName()).withStyle(ChatFormatting.GOLD)),
                    StartupConfig.anchorX(),
                    currentY,
                    0xFFFFFF
            );
            currentY += minecraft.font.lineHeight + spacing;
        }

        if (!StartupConfig.showStartupTime() || totalStartupTime < 0L) return;
        long now = Util.getMillis();
        if (firstTitleScreenRenderTime < 0L) firstTitleScreenRenderTime = now;
        long elapsed = now - firstTitleScreenRenderTime;
        if (elapsed >= 5000L) return;

        float alpha = 1.0F;
        if (elapsed < 500L) alpha = elapsed / 500.0F;
        else if (elapsed > 4500L) alpha = (5000L - elapsed) / 500.0F;
        int alphaInt = (int) (alpha * 255.0F);
        if (alphaInt <= 4) return;

        String seconds = String.format(java.util.Locale.ROOT, "%.2f", totalStartupTime / 1000.0D);
        RenderSystem.enableBlend();
        event.getGuiGraphics().drawString(
                minecraft.font,
                Component.translatable("msg.kineticcore.startup.startup_time", Component.literal(seconds).withStyle(ChatFormatting.GREEN)),
                StartupConfig.anchorX(),
                currentY,
                (alphaInt << 24) | 0xFFFFFF
        );
        RenderSystem.disableBlend();
    }
}
