package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.api.client.overlay.GuiOverlay;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;

public final class GuiOverlayBridge {
    private GuiOverlayBridge() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiOverlay.renderHudLayer(event.getGuiGraphics());
    }

    public static void onRenderScreenPre(ScreenEvent.Render.Pre event) {
        GuiOverlay.beginScreenLayer();
    }

    public static void onRenderScreenPost(ScreenEvent.Render.Post event) {
        GuiOverlay.renderScreenLayer(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
    }
}
