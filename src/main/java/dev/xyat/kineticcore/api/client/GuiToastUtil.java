package dev.xyat.kineticcore.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.xyat.kineticcore.KineticCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = KineticCore.MODID, value = Dist.CLIENT)
public class GuiToastUtil {

    public enum Position {
        TOP_CENTER, BOTTOM_CENTER, CENTER, TOP_LEFT, TOP_RIGHT
    }

    private static class Toast {
        String id;
        Component message;
        long startTime;
        int duration;
        Position position;
        int offsetX;
        int offsetY;

        float currentY = -1000;

        Toast(String id, Component msg, int duration, Position pos, int ox, int oy) {
            this.id = id;
            this.message = msg;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.position = pos;
            this.offsetX = ox;
            this.offsetY = oy;
        }
    }

    private static final List<Toast> activeToasts = new CopyOnWriteArrayList<>();

    public static void showToast(Component msg) {
        showToast(msg.getString(), msg, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    public static void showToast(String id, Component msg) {
        showToast(id, msg, Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    public static void showToast(String id, Component msg, Position position, int durationMs, int offsetX, int offsetY) {
        activeToasts.removeIf(t -> t.id.equals(id) && t.position == position);
        activeToasts.add(new Toast(id, msg, durationMs, position, offsetX, offsetY));
    }

    public static void removeToast(String id) {
        activeToasts.removeIf(t -> t.id.equals(id));
    }

    public static void clearAllToasts() {
        activeToasts.clear();
    }

    private static void doRender(GuiGraphics g, Font font, int screenWidth, int screenHeight) {
        if (activeToasts.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        int currentBottomY = screenHeight - 40;
        int currentTopY = 25;

        for (int i = 0; i < activeToasts.size(); i++) {
            Toast toast = activeToasts.get(i);
            long elapsed = currentTime - toast.startTime;

            if (elapsed > toast.duration) {
                activeToasts.remove(i);
                i--;
                continue;
            }

            MutableComponent boldMsg = toast.message.copy().withStyle(net.minecraft.ChatFormatting.BOLD);
            String rawStr = boldMsg.getString();
            int wrapWidth = Integer.MAX_VALUE;

            if (rawStr.length() > 64) {
                wrapWidth = (int) ((float) font.width(boldMsg) * 64f / rawStr.length());
            }

            List<FormattedCharSequence> lines = font.split(boldMsg, wrapWidth);

            int maxLineWidth = 0;
            for (FormattedCharSequence line : lines) {
                maxLineWidth = Math.max(maxLineWidth, font.width(line));
            }

            int w = maxLineWidth + 16;
            int h = 8 + lines.size() * 10;

            int targetX = 0;
            int targetY = 0;

            switch (toast.position) {
                case TOP_CENTER -> {
                    targetX = (screenWidth - w) / 2;
                    targetY = currentTopY;
                    currentTopY += h + 4;
                }
                case BOTTOM_CENTER -> {
                    targetX = (screenWidth - w) / 2;
                    targetY = currentBottomY;
                    currentBottomY -= (h + 4);
                }
                case CENTER -> {
                    targetX = (screenWidth - w) / 2;
                    targetY = (screenHeight - h) / 2;
                }
                case TOP_LEFT -> {
                    targetX = 10;
                    targetY = currentTopY;
                    currentTopY += h + 4;
                }
                case TOP_RIGHT -> {
                    targetX = screenWidth - w - 10;
                    targetY = currentTopY;
                    currentTopY += h + 4;
                }
            }

            targetX += toast.offsetX;
            targetY += toast.offsetY;

            if (toast.currentY == -1000) {
                if (toast.position == Position.BOTTOM_CENTER) {
                    toast.currentY = targetY + 15;
                } else {
                    toast.currentY = targetY - 15;
                }
            }

            toast.currentY += (targetY - toast.currentY) * 0.25f;

            int renderX = targetX;
            int renderY = Math.round(toast.currentY);

            int fadeInTime = 300;
            int fadeOutTime = 1200;
            float alpha = 1.0f;
            long remaining = toast.duration - elapsed;

            if (remaining < fadeOutTime) {
                alpha = (float) remaining / fadeOutTime;
            } else if (elapsed < fadeInTime) {
                alpha = (float) elapsed / fadeInTime;
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));

            if (alpha <= 0.02f) {
                continue;
            }

            int alphaHex = (int) (alpha * 255.0f) << 24;

            int bgCol = alphaHex | 0x001C1C1C;
            int borderCol = alphaHex | 0x00FFAA00;
            int textCol = alphaHex | 0x00FFFFFF;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            g.pose().pushPose();
            g.pose().translate(0, 0, 800.0f);

            g.fill(renderX + 2, renderY, renderX + w - 2, renderY + h, borderCol);
            g.fill(renderX, renderY + 2, renderX + w, renderY + h - 2, borderCol);
            g.fill(renderX + 1, renderY + 1, renderX + w - 1, renderY + h - 1, borderCol);

            g.fill(renderX + 3, renderY + 2, renderX + w - 3, renderY + h - 2, bgCol);
            g.fill(renderX + 2, renderY + 3, renderX + w - 2, renderY + h - 3, bgCol);

            int textY = renderY + 5;
            for (FormattedCharSequence line : lines) {
                int lineW = font.width(line);
                g.drawString(font, line, renderX + w / 2 - lineW / 2, textY, textCol, false);
                textY += 10;
            }

            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            doRender(event.getGuiGraphics(), mc.font, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        doRender(event.getGuiGraphics(), mc.font, event.getScreen().width, event.getScreen().height);
    }
}