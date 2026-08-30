package dev.xyat.kineticcore.feature.fps.client;

import net.minecraft.ChatFormatting;
import dev.xyat.kineticcore.bootstrap.annotation.KTClientModule;
import dev.xyat.kineticcore.feature.fps.config.FpsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;

import java.util.Arrays;
import java.util.List;

@KTClientModule
public final class FpsRenderer {
    private static final long REFRESH_INTERVAL_NANOS = 500_000_000L;
    private static final long MINIMUM_WINDOW_NANOS = 1_000_000_000L;
    private static final int AVERAGE_SAMPLE_COUNT = 24;
    private static final int[] AVERAGE_SAMPLES = new int[AVERAGE_SAMPLE_COUNT];

    private static boolean registered;
    private static boolean hasStats;
    private static boolean averageFilled;
    private static long lastRefreshNanos;
    private static int averageIndex;
    private static int cachedCurrentFps;
    private static int cachedAverageFps;
    private static int cachedMinimumFps;

    private FpsRenderer() {
    }

    public static void register() {
        if (registered) return;
        MinecraftForge.EVENT_BUS.addListener(FpsRenderer::onRenderOverlay);
        registered = true;
    }

    public static List<Component> createPreviewLines() {
        FpsStats stats = getFpsStats(Minecraft.getInstance());
        return List.of(createFpsText(stats.current(), stats.minimum(), stats.average()));
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
        if (minecraft.screen instanceof FpsHudEditorScreen) return;
        if (!FpsClientConfig.isHudEnabled()
                || minecraft.options.hideGui
                || minecraft.level == null
                || minecraft.options.renderDebug) return;

        FpsStats stats = getFpsStats(minecraft);
        List<Component> lines = List.of(createFpsText(stats.current(), stats.minimum(), stats.average()));
        int contentWidth = getContentWidth(minecraft.font, lines);
        int contentHeight = getContentHeight(minecraft.font, lines.size());
        double scale = FpsClientConfig.getHudScale();
        int scaledWidth = scaledSize(contentWidth, scale);
        int scaledHeight = scaledSize(contentHeight, scale);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = Mth.clamp(screenWidth - scaledWidth - 2 - FpsClientConfig.getHudOffsetX(), 0, Math.max(0, screenWidth - scaledWidth));
        int y = Mth.clamp(screenHeight - scaledHeight - 2 - FpsClientConfig.getHudOffsetY(), 0, Math.max(0, screenHeight - scaledHeight));

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        renderLines(graphics, minecraft.font, lines, 0, 0);
        graphics.pose().popPose();
    }

    private static Component createFpsText(int current, int minimum, int average) {
        return Component.empty()
                .append(Component.translatable("gui.kineticcore.fps.label.fps"))
                .append(formatFpsValue(current))
                .append(Component.translatable("gui.kineticcore.fps.label.minimum"))
                .append(formatFpsValue(minimum))
                .append(Component.translatable("gui.kineticcore.fps.label.average"))
                .append(formatFpsValue(average));
    }

    private static Component formatFpsValue(int fps) {
        ChatFormatting color;
        if (fps >= 60) color = ChatFormatting.GREEN;
        else if (fps >= 40) color = ChatFormatting.YELLOW;
        else if (fps >= 20) color = ChatFormatting.GOLD;
        else color = ChatFormatting.RED;
        return Component.literal(String.valueOf(fps)).withStyle(color);
    }

    private static FpsStats getFpsStats(Minecraft minecraft) {
        long now = System.nanoTime();
        if (!hasStats || now - lastRefreshNanos >= REFRESH_INTERVAL_NANOS) {
            refreshFpsStats(minecraft, now);
        }
        return new FpsStats(cachedCurrentFps, cachedMinimumFps, cachedAverageFps);
    }

    private static void refreshFpsStats(Minecraft minecraft, long now) {
        int current = minecraft.getFps();
        if (current <= 0) {
            current = hasStats ? Math.max(1, cachedCurrentFps) : 1;
        }

        cachedCurrentFps = current;
        cachedMinimumFps = calculateMinimumFps(minecraft, current);
        pushAverageFps(current);
        cachedAverageFps = calculateAverageFps();
        lastRefreshNanos = now;
        hasStats = true;
    }

    private static int calculateMinimumFps(Minecraft minecraft, int currentFps) {
        FrameTimer timer = minecraft.getFrameTimer();
        int start = timer.getLogStart();
        int end = timer.getLogEnd();
        if (end == start) {
            return hasStats ? Math.max(1, cachedMinimumFps) : currentFps;
        }

        long[] frames = timer.getLog();
        long maximumFrameNanos = Math.max(1L, (long) (1_000_000_000.0D / Math.max(1, currentFps)));
        long totalFrameNanos = 0L;
        int index = Math.floorMod(end - 1, frames.length);

        while (index != start && totalFrameNanos < MINIMUM_WINDOW_NANOS) {
            long frameNanos = frames[index];
            if (frameNanos > maximumFrameNanos) {
                maximumFrameNanos = frameNanos;
            }
            totalFrameNanos += frameNanos;
            index = Math.floorMod(index - 1, frames.length);
        }

        return clampFps((long) (1_000_000_000.0D / maximumFrameNanos));
    }

    private static void pushAverageFps(int fps) {
        if (averageIndex == AVERAGE_SAMPLES.length) {
            averageIndex = 0;
            averageFilled = true;
        }

        if (!averageFilled) {
            Arrays.fill(AVERAGE_SAMPLES, averageIndex, AVERAGE_SAMPLES.length, fps);
        }

        AVERAGE_SAMPLES[averageIndex++] = fps;
    }

    private static int calculateAverageFps() {
        long total = 0L;
        for (int fps : AVERAGE_SAMPLES) {
            total += fps;
        }
        return clampFps(Math.round(total / (double) AVERAGE_SAMPLES.length));
    }

    private static int clampFps(long fps) {
        return (int) Math.max(1L, Math.min(100000L, fps));
    }

    private static int scaledSize(int baseSize, double scale) {
        double result = Math.ceil(baseSize * scale);
        if (!Double.isFinite(result) || result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) result);
    }

    private record FpsStats(int current, int minimum, int average) {
    }
}
