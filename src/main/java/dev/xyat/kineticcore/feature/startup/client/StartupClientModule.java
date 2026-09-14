package dev.xyat.kineticcore.feature.startup.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.feature.startup.config.StartupConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.lang.management.ManagementFactory;

public final class StartupClientModule {
    private static long totalStartupTime = -1L;
    private static long firstTitleScreenRenderTime = -1L;
    private static boolean calculated;

    private StartupClientModule() {
    }

    public static void register() {
        KineticClientEvents.onScreenInitBefore(StartupClientModule::onScreenInit);
        KineticClientEvents.onScreenRenderAfter(StartupClientModule::onScreenRender);
    }

    private static void onScreenInit(Screen screen) {
        if (calculated) return;
        calculated = true;
        long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        totalStartupTime = System.currentTimeMillis() - jvmStartTime;
    }

    private static void onScreenRender(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!(screen instanceof TitleScreen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        int currentY = StartupConfig.anchorY();
        int spacing = 2;

        if (StartupConfig.showLoginInfo()) {
            User user = minecraft.getUser();
            graphics.drawString(
                    minecraft.font,
                    KineticText.translatable("msg.kineticcore.startup.account_id", Component.literal(user.getName())),
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
        graphics.drawString(
                minecraft.font,
                KineticText.translatable("msg.kineticcore.startup.startup_time", Component.literal(seconds)),
                StartupConfig.anchorX(),
                currentY,
                (alphaInt << 24) | 0xFFFFFF
        );
        RenderSystem.disableBlend();
    }
}
