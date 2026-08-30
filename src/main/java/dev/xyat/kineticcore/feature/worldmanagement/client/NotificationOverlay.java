package dev.xyat.kineticcore.feature.worldmanagement.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "kineticcore", value = Dist.CLIENT)
public class NotificationOverlay {
    private static final List<NotificationEntry> NOTIFICATIONS = new ArrayList<>();

    public static void addNotification(Component text) {
        addNotification(text, false);
    }

    public static void addNotification(Component text, boolean permanent) {
        synchronized (NOTIFICATIONS) {
            NOTIFICATIONS.add(new NotificationEntry(text, permanent));
        }
    }

    public static void removeNotification(Component text) {
        synchronized (NOTIFICATIONS) {
            NOTIFICATIONS.removeIf(entry -> entry.text.getString().equals(text.getString()));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        render(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        render(event.getGuiGraphics());
    }

    private static void render(GuiGraphics graphics) {
        if (!NOTIFICATIONS.isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            long now = System.currentTimeMillis();
            int screenWidth = client.getWindow().getGuiScaledWidth();
            int currentY = 10;
            synchronized (NOTIFICATIONS) {
                Iterator<NotificationEntry> it = NOTIFICATIONS.iterator();

                while (it.hasNext()) {
                    NotificationEntry entry = it.next();
                    if (!entry.permanent && now - entry.startTime > 3000L) {
                        it.remove();
                    } else {
                        int textWidth = client.font.width(entry.text);
                        graphics.drawString(client.font, entry.text, screenWidth - textWidth - 10, currentY, 16777215, true);
                        currentY += 9 + 2;
                    }
                }
            }
        }
    }

    private static class NotificationEntry {
        final long startTime;
        final Component text;
        final boolean permanent;

        NotificationEntry(Component text, boolean permanent) {
            this.text = text;
            this.permanent = permanent;
            this.startTime = System.currentTimeMillis();
        }
    }
}