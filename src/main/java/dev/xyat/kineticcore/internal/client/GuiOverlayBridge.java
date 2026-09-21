package dev.xyat.kineticcore.internal.client;

import dev.xyat.kineticcore.internal.client.overlay.GuiOverlayRuntime;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;

public final class GuiOverlayBridge {
    private GuiOverlayBridge() {
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiOverlayRuntime.renderHudLayer(event.getGuiGraphics());
    }

    public static void onRenderScreenPre(ScreenEvent.Render.Pre event) {
        GuiOverlayRuntime.beginScreenLayer();
    }

    public static void onRenderScreenPost(ScreenEvent.Render.Post event) {
        GuiOverlayRuntime.renderScreenLayer(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
    }
}
