package dev.xyat.kineticcore.feature.tps.client;

import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.selector.KineticHudEditorPage;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.feature.tps.config.TpsClientConfig;
import dev.xyat.kineticcore.feature.tps.config.TpsConfigGui;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Tps HUD 位置编辑页 / Tps HUD position editor page. */
public final class TpsHudEditorPage extends KineticHudEditorPage {
    private final List<Component> previewLines = TpsRenderer.createPreviewLines();

    public TpsHudEditorPage() {
        super(KineticI18n.translatable("screen.kineticcore.tps.editor.title"));
    }

    @Override
    protected int elementWidth() {
        return TpsRenderer.getContentWidth(previewLines);
    }

    @Override
    protected int elementHeight() {
        return TpsRenderer.getContentHeight(previewLines.size());
    }

    @Override
    protected HudLayout initialLayout(int screenWidth, int screenHeight) {
        double scale = TpsClientConfig.getHudScale();
        int defaultX = screenWidth - scaledSize(elementWidth(), scale) - 2;
        int defaultY = screenHeight - scaledSize(elementHeight(), scale) - 2;
        return new HudLayout(
                defaultX - TpsClientConfig.getHudOffsetX(),
                defaultY - (TpsClientConfig.getHudOffsetY()),
                scale);
    }

    @Override
    protected HudLayout defaultLayout(int screenWidth, int screenHeight) {
        return new HudLayout(screenWidth - elementWidth() - 2, screenHeight - elementHeight() - 2 - 0, 1.0D);
    }

    @Override
    protected void renderElement(KineticGraphics graphics, int x, int y, int mouseX, int mouseY) {
        TpsRenderer.renderLines(graphics, previewLines, x, y);
    }

    @Override
    protected Component positionText(HudLayout layout) {
        return super.positionText(new HudLayout(offsetX(layout), offsetY(layout), layout.scale()));
    }

    @Override
    protected void save(HudLayout layout) {
        TpsClientConfig.setHudLayout(offsetX(layout), offsetY(layout), layout.scale());
        KTConfigApi.find(TpsConfigGui.PAGE_ID).ifPresent(KTConfigApi::notifySaved);
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
