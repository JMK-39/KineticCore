package dev.xyat.kineticcore.feature.fps.client;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.selector.KineticHudEditorPage;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.fps.config.FpsClientConfig;
import dev.xyat.kineticcore.feature.fps.config.FpsConfigGui;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Fps HUD 位置编辑页 / Fps HUD position editor page. */
public final class FpsHudEditorPage extends KineticHudEditorPage {
    private final List<Component> previewLines = FpsRenderer.createPreviewLines();

    public FpsHudEditorPage() {
        super(KineticI18n.translatable("screen.kineticcore.fps.editor.title"));
    }

    @Override
    protected int elementWidth() {
        return FpsRenderer.getContentWidth(previewLines);
    }

    @Override
    protected int elementHeight() {
        return FpsRenderer.getContentHeight(previewLines.size());
    }

    @Override
    protected HudLayout initialLayout(int screenWidth, int screenHeight) {
        double scale = FpsClientConfig.getHudScale();
        int defaultX = screenWidth - scaledSize(elementWidth(), scale) - 2;
        int defaultY = screenHeight - scaledSize(elementHeight(), scale) - 2 - FpsClientConfig.DEFAULT_OFFSET_Y;
        return new HudLayout(
                defaultX - FpsClientConfig.getHudOffsetX(),
                defaultY - (FpsClientConfig.getHudOffsetY() - FpsClientConfig.DEFAULT_OFFSET_Y),
                scale);
    }

    @Override
    protected HudLayout defaultLayout(int screenWidth, int screenHeight) {
        return new HudLayout(screenWidth - elementWidth() - 2, screenHeight - elementHeight() - 2 - FpsClientConfig.DEFAULT_OFFSET_Y, 1.0D);
    }

    @Override
    protected void renderElement(KineticGraphics graphics, int x, int y, int mouseX, int mouseY) {
        FpsRenderer.renderLines(graphics, previewLines, x, y);
    }

    @Override
    protected Component positionText(HudLayout layout) {
        return super.positionText(new HudLayout(offsetX(layout), offsetY(layout), layout.scale()));
    }

    @Override
    protected void save(HudLayout layout) {
        FpsClientConfig.setHudLayout(offsetX(layout), offsetY(layout), layout.scale());
        KTConfigApi.find(FpsConfigGui.PAGE_ID).ifPresent(KTConfigApi::notifySaved);
        KTConfigApi.refreshOpenScreens();
    }

    private int offsetX(HudLayout layout) {
        return width() - scaledElementWidth() - 2 - layout.x();
    }

    private int offsetY(HudLayout layout) {
        return height() - scaledElementHeight() - 2 - layout.y();
    }

    private static int scaledSize(int baseSize, double scale) {
        double result = Math.ceil(baseSize * scale);
        if (!Double.isFinite(result) || result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) result);
    }
}
