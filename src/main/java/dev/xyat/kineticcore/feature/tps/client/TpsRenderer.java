package dev.xyat.kineticcore.feature.tps.client;

import net.minecraft.ChatFormatting;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.feature.tps.config.TpsClientConfig;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Locale;

@KTClientModule
public final class TpsRenderer {
    private static final long DATA_TIMEOUT_MILLIS = 5000L;

    private static boolean registered;
    private static boolean hasData;
    private static double cachedTps = 20.0D;
    private static double cachedMspt;
    private static long lastUpdateMillis;

    private TpsRenderer() {
    }

    public static void register() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.addListener(TpsRenderer::onRenderOverlay);
        MinecraftForge.EVENT_BUS.addListener(TpsRenderer::onClientLogin);
        registered = true;
    }

    private static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        TpsNetwork.sendSubscription(TpsClientConfig.isHudEnabled());
    }

    public static void updateData(double tps, double mspt) {
        cachedTps = tps;
        cachedMspt = mspt;
        hasData = true;
        lastUpdateMillis = System.currentTimeMillis();
    }

    public static List<Component> createPreviewLines() {
        return List.of(createTpsText(cachedTps, cachedMspt));
    }

    public static int getContentWidth(Font font, List<Component> lines) {
        int width = 1;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        return width;
    }

    public static int getContentHeight(Font font, int lineCount) {
        return lineCount <= 0 ? 1 : lineCount * font.lineHeight;
    }

    public static void renderLines(GuiGraphics graphics, Font font, List<Component> lines, int x, int y) {
        int lineY = y;
        for (Component line : lines) {
            graphics.drawString(font, line, x, lineY, 0xFFFFFF, true);
            lineY += font.lineHeight;
        }
    }

    private static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TpsHudEditorScreen) return;
        if (!TpsClientConfig.isHudEnabled()
                || !hasData
                || System.currentTimeMillis() - lastUpdateMillis > DATA_TIMEOUT_MILLIS) return;
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.options.renderDebug) return;

        List<Component> lines = List.of(createTpsText(cachedTps, cachedMspt));
        int contentWidth = getContentWidth(minecraft.font, lines);
        int contentHeight = getContentHeight(minecraft.font, lines.size());
        double scale = TpsClientConfig.getHudScale();
        int scaledWidth = scaledSize(contentWidth, scale);
        int scaledHeight = scaledSize(contentHeight, scale);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = Mth.clamp(screenWidth - scaledWidth - 2 - TpsClientConfig.getHudOffsetX(), 0, Math.max(0, screenWidth - scaledWidth));
        int y = Mth.clamp(screenHeight - scaledHeight - 2 - TpsClientConfig.getHudOffsetY(), 0, Math.max(0, screenHeight - scaledHeight));

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        renderLines(graphics, minecraft.font, lines, 0, 0);
        graphics.pose().popPose();
    }

    private static Component createTpsText(double tps, double mspt) {
        return Component.empty()
                .append(Component.translatable("gui.kineticcore.tps.label.tps"))
                .append(formatTpsValue(String.format(Locale.ROOT, "%.1f", tps), tps))
                .append(Component.translatable("gui.kineticcore.tps.separator"))
                .append(Component.translatable("gui.kineticcore.tps.label.mspt"))
                .append(formatMsptValue(String.format(Locale.ROOT, "%.1f", mspt), mspt));
    }

    private static Component formatTpsValue(String value, double tps) {
        ChatFormatting color;
        if (tps >= 18.0D) color = ChatFormatting.GREEN;
        else if (tps >= 15.0D) color = ChatFormatting.YELLOW;
        else if (tps >= 10.0D) color = ChatFormatting.GOLD;
        else color = ChatFormatting.RED;
        return Component.literal(value).withStyle(color);
    }

    private static Component formatMsptValue(String value, double mspt) {
        ChatFormatting color;
        if (mspt < 30.0D) color = ChatFormatting.GREEN;
        else if (mspt < 40.0D) color = ChatFormatting.YELLOW;
        else if (mspt < 50.0D) color = ChatFormatting.GOLD;
        else color = ChatFormatting.RED;
        return Component.literal(value).withStyle(color);
    }

    private static int scaledSize(int baseSize, double scale) {
        double result = Math.ceil(baseSize * scale);
        if (!Double.isFinite(result) || result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) result);
    }
}
