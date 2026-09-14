package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.screen.GuiSession;
import net.minecraftforge.client.event.ScreenEvent;

public final class GuiSessionBridge {
    private GuiSessionBridge() {
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        GuiSession.handleScreenOpening(event.getCurrentScreen(), event.getNewScreen());
    }

    public static void onPlainScreenEscape(ScreenEvent.KeyPressed.Pre event) {
        if (GuiSession.handlePlainScreenEscape(event.getScreen(), event.getKeyCode())) {
            event.setCanceled(true);
        }
    }
}
