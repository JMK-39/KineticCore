package dev.xyat.kineticcore.internal.client.editor;

import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.command.KineticCommandSuggestions;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.widget.input.KineticTextFields.KineticEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
final class CommandEditorScreen extends KineticScreen {
    private final CommandListEditorScreen parent;
    private final int editingIndex;
    private KineticEditBox input;
    private KineticCommandSuggestions.Session commandSuggestions;

    CommandEditorScreen(CommandListEditorScreen parent, int editingIndex) {
        super(parent.editorTitle(editingIndex));
        this.parent = parent;
        this.editingIndex = editingIndex;
    }

    @Override
    protected void buildUi() {
        String initial = "";
        if (editingIndex >= 0 && editingIndex < parent.currentCommands().size()) {
            initial = parent.currentCommands().get(editingIndex);
        }

        input = addTextField(
                44,
                278,
                552,
                KineticText.translatable("gui.kineticcore.command_edit.input"), null, null, null
        );
        input.setCanLoseFocus(false);
        input.setMaxLength(2048);
        input.setValue(toEditorText(initial));
        focusControl(input);

        commandSuggestions = KineticCommandSuggestions.create(
                input,
                canvasWidth(),
                canvasHeight(),
                KineticCommandSuggestions.Options.fieldAligned(false, false, 10, 0xD0000000)
        );
        commandSuggestions.setAllowSuggestions(true);
        input.setResponder(value -> commandSuggestions.update());
        commandSuggestions.update();

        addButton(
                208,
                314,
                96,
                KineticText.translatable("gui.kineticcore.command_edit.save"),
                null,
                this::saveCommand
        );
        addButton(
                336,
                314,
                96,
                KineticText.translatable("gui.kineticcore.command_edit.back"),
                null,
                this::closeToParent
        );
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, canvasWidth(), canvasHeight(), 0xFF171717, 0xFF0E0E0E);
        GuiTheme.panel(graphics, 24, 18, 592, 324);
        graphics.drawCenteredString(font, title, canvasWidth() / 2, 30, 0xFFFFFF);
        graphics.drawString(
                font,
                KineticText.translatable("gui.kineticcore.command_edit.hint"),
                44,
                56,
                0xFFFFFF,
                false
        );
        if (parent.variableHint() != null) {
            graphics.drawString(font, parent.variableHint(), 44, 76, 0xFFFFFF, false);
        }
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (commandSuggestions != null) {
            commandSuggestions.render(graphics, mouseX, mouseY);
        }
    }

    private String toEditorText(String stored) {
        if (stored == null || stored.isBlank()) return "/";
        String trimmed = stored.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private String normalizeForStorage(String text) {
        if (text == null) return "";
        String normalized = text.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private void saveCommand() {
        String command = normalizeForStorage(input.getValue());
        if (command.isBlank()) {
            KineticOverlays.toast(null, KineticText.translatable("msg.kineticcore.command_edit.empty"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
            return;
        }

        try {
            parent.saveEditedCommand(editingIndex, command);
            closeToParent();
        } catch (Throwable throwable) {
            KineticOverlays.toast(null, parent.saveFailedMessage(), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }

    @Override
    protected boolean canvasKeyPressed(int keyCode, int scanCode, int modifiers) {
        return commandSuggestions != null && commandSuggestions.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (commandSuggestions != null && commandSuggestions.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (commandSuggestions != null && commandSuggestions.mouseScrolled(delta)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    private void closeToParent() {
        navigateBack();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
