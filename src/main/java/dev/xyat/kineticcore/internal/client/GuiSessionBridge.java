package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.client.screen.GuiSessionRuntime;
import net.minecraftforge.client.event.ScreenEvent;

public final class GuiSessionBridge {
    private GuiSessionBridge() {
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        GuiSessionRuntime.handleScreenOpening(event.getCurrentScreen(), event.getNewScreen());
    }

    public static void onPlainScreenEscape(ScreenEvent.KeyPressed.Pre event) {
        if (GuiSessionRuntime.handlePlainScreenEscape(event.getScreen(), event.getKeyCode())) {
            event.setCanceled(true);
        }
    }
}
