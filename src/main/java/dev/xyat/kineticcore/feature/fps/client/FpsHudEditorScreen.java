package dev.xyat.kineticcore.feature.fps.client;

import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.selector.HudPositionEditor;
import dev.xyat.kineticcore.api.client.screen.KineticNativeScreen;
import dev.xyat.kineticcore.api.config.client.KTConfigApi;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.feature.fps.config.FpsClientConfig;
import dev.xyat.kineticcore.feature.fps.config.FpsConfigGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FpsHudEditorScreen extends KineticNativeScreen {
    private final Screen parent;
    private final List<Component> previewLines;
    private final HudPositionEditor editor = new HudPositionEditor();

    public FpsHudEditorScreen(Screen parent) {
        super(KineticText.translatable("screen.kineticcore.fps.editor.title"));
        this.parent = parent;
        this.previewLines = FpsRenderer.createPreviewLines();
    }

    @Override
    protected void buildUi() {
        int previewWidth = FpsRenderer.getContentWidth(font, previewLines);
        int previewHeight = FpsRenderer.getContentHeight(font, previewLines.size());
        double initialScale = FpsClientConfig.getHudScale();
        int initialScaledWidth = scaledSize(previewWidth, initialScale);
        int initialScaledHeight = scaledSize(previewHeight, initialScale);
        int initialDefaultX = width - initialScaledWidth - 2;
        int initialDefaultY = height - initialScaledHeight - 2 - FpsClientConfig.DEFAULT_OFFSET_Y;
        int defaultX = width - previewWidth - 2;
        int defaultY = height - previewHeight - 2 - FpsClientConfig.DEFAULT_OFFSET_Y;

        editor.initialize(
                width,
                height,
                previewWidth,
                previewHeight,
                initialDefaultX - FpsClientConfig.getHudOffsetX(),
                initialDefaultY - (FpsClientConfig.getHudOffsetY() - FpsClientConfig.DEFAULT_OFFSET_Y),
                defaultX,
                defaultY,
                initialScale,
                1.0D,
                HudPositionEditor.DEFAULT_MINIMUM_SCALE
        );

        editor.addControlButtons(
                widget -> addControl(widget, null),
                KineticText.translatable("gui.kineticcore.hud_editor.save"),
                KineticText.translatable("gui.kineticcore.hud_editor.reset"),
                KineticText.translatable("gui.kineticcore.hud_editor.cancel"),
                this::save,
                this::closeWithoutSaving
        );
    }

    @Override
    protected void renderNativeBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        editor.render(
                graphics,
                font,
                mouseX,
                mouseY,
                title,
                KineticText.translatable("screen.kineticcore.hud_editor.instruction_scale"),
                KineticText.translatable(
                        "screen.kineticcore.hud_editor.position_scale",
                        Component.literal(String.valueOf(currentOffsetX())),
                        Component.literal(String.valueOf(currentOffsetY())),
                        Component.literal(String.valueOf(Math.round(editor.getScale() * 100.0D)))
                ),
                (g, x, y, mx, my) -> FpsRenderer.renderLines(g, font, previewLines, x, y)
        );
    }

    @Override
    protected boolean nativeMouseClicked(double mouseX, double mouseY, int button) {
        return editor.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean nativeMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return editor.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    protected boolean nativeMouseReleased(double mouseX, double mouseY, int button) {
        return editor.mouseReleased(button);
    }

    @Override
    protected boolean nativeMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return editor.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    protected boolean nativeKeyPressed(int keyCode, int scanCode, int modifiers) {
        return editor.keyPressed(keyCode, KineticClientRuntime.shiftModifierDown());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        FpsClientConfig.setHudLayout(currentOffsetX(), currentOffsetY(), editor.getScale());
        KTConfigApi.find(FpsConfigGui.PAGE_ID).ifPresent(KTConfigApi::notifySaved);
        KTConfigApi.refreshScreenFromSource(parent);
    }

    private void closeWithoutSaving() {
        closeScreen();
    }

    private void closeScreen() {
        navigateBack();
    }

    private int currentOffsetX() {
        return width - editor.getElementWidth() - 2 - editor.getX();
    }

    private int currentOffsetY() {
        return height - editor.getElementHeight() - 2 - editor.getY();
    }

    private static int scaledSize(int baseSize, double scale) {
        double result = Math.ceil(baseSize * scale);
        if (!Double.isFinite(result) || result >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return Math.max(1, (int) result);
    }
}
