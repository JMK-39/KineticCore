package dev.xyat.kineticcore.feature.worldmanagement.client;

import dev.xyat.kineticcore.api.runtime.KineticRegistrationBatch;
import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class NotificationOverlay {
    private static final KineticRegistrationBatch REGISTRATION = new KineticRegistrationBatch();

    public static void register() {
        REGISTRATION.run(
                () -> KineticClientEvents.onHudRender(KineticClientEvents.HudStage.END, (graphics, partialTick) -> render(graphics)),
                () -> KineticClientEvents.onScreenRenderAfter((screen, graphics, mouseX, mouseY, partialTick) -> render(graphics))
        );
    }

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

    private static void render(GuiGraphics graphics) {
        if (!NOTIFICATIONS.isEmpty()) {
            var font = KineticClientRuntime.font();
            long now = System.currentTimeMillis();
            int screenWidth = KineticClientRuntime.guiScaledWidth();
            int currentY = 10;
            synchronized (NOTIFICATIONS) {
                Iterator<NotificationEntry> it = NOTIFICATIONS.iterator();

                while (it.hasNext()) {
                    NotificationEntry entry = it.next();
                    if (!entry.permanent && now - entry.startTime > 3000L) {
                        it.remove();
                    } else {
                        int textWidth = font.width(entry.text);
                        graphics.drawString(font, entry.text, screenWidth - textWidth - 10, currentY, GuiTheme.current().text(), true);
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