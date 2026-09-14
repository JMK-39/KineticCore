package dev.xyat.kineticcore.feature.tps.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.feature.tps.config.TpsClientConfig;
import dev.xyat.kineticcore.feature.tps.network.TpsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

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
        KineticClientEvents.onHudRender(KineticClientEvents.HudStage.AFTER_CHAT, TpsRenderer::onRenderOverlay);
        KineticClientEvents.onLogin(TpsRenderer::onClientLogin);
        registered = true;
    }

    private static void onClientLogin() {
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

    private static void onRenderOverlay(GuiGraphics graphics, float partialTick) {
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
        String key;
        if (tps >= 18.0D) key = "msg.kineticcore.metric.good";
        else if (tps >= 15.0D) key = "msg.kineticcore.metric.warning";
        else if (tps >= 10.0D) key = "msg.kineticcore.metric.caution";
        else key = "msg.kineticcore.metric.bad";
        return KineticText.translatable(key, Component.literal(value));
    }

    private static Component formatMsptValue(String value, double mspt) {
        String key;
        if (mspt < 30.0D) key = "msg.kineticcore.metric.good";
        else if (mspt < 40.0D) key = "msg.kineticcore.metric.warning";
        else if (mspt < 50.0D) key = "msg.kineticcore.metric.caution";
        else key = "msg.kineticcore.metric.bad";
        return KineticText.translatable(key, Component.literal(value));
    }

    private static int scaledSize(int baseSize, double scale) {
        double result = Math.ceil(baseSize * scale);
        if (!Double.isFinite(result) || result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) result);
    }
}
