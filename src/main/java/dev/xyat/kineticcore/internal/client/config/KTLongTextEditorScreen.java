package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.*;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

final class KTLongTextEditorScreen extends KineticScreen {
    private static final int CONTENT_W = 480;
    private static final int CONTENT_H = 280;
    private static final int CONTENT_X = (STANDARD_CANVAS_WIDTH - CONTENT_W) / 2;
    private static final int CONTENT_Y = (STANDARD_CANVAS_HEIGHT - CONTENT_H) / 2;
    private static final int PANEL_X = CONTENT_X + 18;
    private static final int PANEL_Y = CONTENT_Y + 18;
    private static final int PANEL_W = 444;
    private static final int PANEL_H = 244;
    private static final int EDIT_X = CONTENT_X + 32;
    private static final int EDIT_Y = CONTENT_Y + 54;
    private static final int EDIT_W = 416;
    private static final int EDIT_H = 156;

    private final Screen parent;
    private final Consumer<String> onApply;
    private String draftValue;
    private MultiLineEditBox editor;

    KTLongTextEditorScreen(Screen parent, Component title, String initialValue, Consumer<String> onApply) {
        super(Objects.requireNonNull(title, "title"));
        this.parent = parent;
        this.draftValue = initialValue == null ? "" : initialValue;
        this.onApply = Objects.requireNonNull(onApply, "onApply");
    }

    @Override
    protected void buildUi() {
        if (editor != null) draftValue = editor.getValue();
        editor = addMultiLineTextField(
                EDIT_X, EDIT_Y, EDIT_W, EDIT_H,
                title, Component.empty(), null
        );
        editor.setCharacterLimit(32767);
        editor.setValue(draftValue);
        editor.setValueListener(value -> draftValue = value == null ? "" : value);
        focusControl(editor);

        addButton(
                PANEL_X + PANEL_W - 154, PANEL_Y + PANEL_H - 30, 68,
                Component.translatable("gui.kineticcore.config.long_text.cancel"),
                null,
                this::onClose
        );
        addButton(
                PANEL_X + PANEL_W - 78, PANEL_Y + PANEL_H - 30, 68,
                Component.translatable("gui.kineticcore.config.long_text.apply"),
                null,
                this::applyAndClose
        );
    }

    @Override
    public void tick() {
        super.tick();
        if (editor != null) editor.tick();
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.shadow(graphics, canvasWidth(), canvasHeight());
        GuiTheme.panel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        KineticText.drawScrollingCentered(
                graphics,
                font,
                title,
                CONTENT_X + CONTENT_W / 2,
                PANEL_Y + 12,
                PANEL_W - 28,
                GuiTheme.current().accentHover(),
                false
        );
    }

    private void applyAndClose() {
        if (editor != null) draftValue = editor.getValue();
        onApply.accept(draftValue);
        onClose();
    }

    @Override
    public void onClose() {
        KineticClientRuntime.openScreen(parent);
    }
}
