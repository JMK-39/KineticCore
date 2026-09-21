package dev.xyat.kineticcore.internal.client.editor;

import dev.xyat.kineticcore.api.client.editor.KineticCommandListEditor;
import dev.xyat.kineticcore.api.client.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import dev.xyat.kineticcore.api.client.text.KineticText;
import dev.xyat.kineticcore.api.client.theme.GuiTheme;
import dev.xyat.kineticcore.api.client.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.api.config.client.KTServerConfigClient;
import net.minecraft.client.gui.GuiGraphics;
import dev.xyat.kineticcore.api.client.widget.button.KineticButtons.StateButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public final class CommandListEditorScreen extends KineticScreen {
    private static final int PANEL_X = 24;
    private static final int PANEL_Y = 18;
    private static final int PANEL_W = 592;
    private static final int PANEL_H = 324;
    private static final int LIST_X = 44;
    private static final int LIST_Y = 58;
    private static final int LIST_W = 538;
    private static final int ROW_H = 24;
    private static final int VISIBLE_ROWS = 10;
    private static final int LIST_H = ROW_H * VISIBLE_ROWS;
    private static final int SCROLL_X = LIST_X + LIST_W + 6;
    private static final int SCROLL_W = 4;
    private static final int MOVE_W = 34;
    private static final int DELETE_W = 54;
    private static final int BUTTON_GAP = 3;

    private final Supplier<List<String>> commandGetter;
    private final Consumer<List<String>> commandSetter;
    private final String serverPageId;
    private final String serverEntryId;
    private final KineticCommandListEditor.Text text;
    private final GridScrollController scroll = new GridScrollController();
    private final List<StateButton> upButtons = new ArrayList<>();
    private final List<StateButton> downButtons = new ArrayList<>();
    private final List<StateButton> deleteButtons = new ArrayList<>();
    private List<Component> deferredTooltip;

    public CommandListEditorScreen(
            Screen parent,
            Supplier<List<String>> commandGetter,
            Consumer<List<String>> commandSetter,
            String serverPageId,
            String serverEntryId,
            KineticCommandListEditor.Text text
    ) {
        super(text.title());
        this.commandGetter = commandGetter;
        this.commandSetter = commandSetter;
        this.serverPageId = serverPageId;
        this.serverEntryId = serverEntryId;
        this.text = text;
        this.commandSetter.accept(KTServerConfigClient.getStringList(serverPageId, serverEntryId, commandGetter.get()));
    }

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        upButtons.clear();
        downButtons.clear();
        deleteButtons.clear();
        updateScrollRange();

        int deleteX = LIST_X + LIST_W - DELETE_W - 4;
        int downX = deleteX - BUTTON_GAP - MOVE_W;
        int upX = downX - BUTTON_GAP - MOVE_W;
        List<String> commands = currentCommands();
        for (int index = 0; index < commands.size(); index++) {
            final int commandIndex = index;
            int y = LIST_Y + index * ROW_H + 2;
            upButtons.add(addCompactScrollableButton(
                    upX, y, MOVE_W,
                    KineticText.translatable("gui.kineticcore.command_list.move_up"), null,
                    () -> moveIndex(commandIndex, -1),
                    LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H,
                    () -> scroll.smoothOffset() * ROW_H
            ));
            downButtons.add(addCompactScrollableButton(
                    downX, y, MOVE_W,
                    KineticText.translatable("gui.kineticcore.command_list.move_down"), null,
                    () -> moveIndex(commandIndex, 1),
                    LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H,
                    () -> scroll.smoothOffset() * ROW_H
            ));
            deleteButtons.add(addCompactScrollableButton(
                    deleteX, y, DELETE_W,
                    KineticText.translatable("gui.kineticcore.command_list.delete"), null,
                    () -> deleteCommand(commandIndex),
                    LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H,
                    () -> scroll.smoothOffset() * ROW_H
            ));
        }

        addButton(
                44, 314, 110,
                KineticText.translatable("gui.kineticcore.command_list.add"),
                null,
                () -> openEditor(-1)
        );
        addButton(
                472, 314, 110,
                KineticText.translatable("gui.kineticcore.command_list.back"),
                null,
                this::closeToParent
        );
        updateRowButtons();
    }

    List<String> currentCommands() {
        List<String> commands = commandGetter.get();
        return commands == null ? List.of() : commands;
    }

    Component editorTitle(int editingIndex) {
        return editingIndex >= 0 ? text.editEditorTitle() : text.addEditorTitle();
    }

    Component variableHint() {
        return text.variableHint();
    }

    void saveEditedCommand(int editingIndex, String command) {
        List<String> updated = new ArrayList<>(currentCommands());
        if (editingIndex >= 0 && editingIndex < updated.size()) {
            updated.set(editingIndex, command);
        } else {
            updated.add(command);
        }
        persist(updated);
        refreshAfterEdit();
        KineticOverlays.toast(null, text.savedMessage(), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
    }

    void refreshAfterEdit() {
        updateScrollRange();
        int lastIndex = Math.max(0, currentCommands().size() - 1);
        if (lastIndex >= scroll.smoothOffset() + VISIBLE_ROWS) {
            scroll.setOffset(lastIndex - VISIBLE_ROWS + 1);
        }
        updateRowButtons();
    }

    Component saveFailedMessage() {
        return text.saveFailedMessage();
    }

    private void updateScrollRange() {
        scroll.update(currentCommands().size(), VISIBLE_ROWS);
    }

    private void updateRowButtons() {
        int size = currentCommands().size();
        int buttonCount = Math.min(size, Math.min(upButtons.size(), Math.min(downButtons.size(), deleteButtons.size())));
        for (int index = 0; index < upButtons.size(); index++) {
            boolean visible = index < buttonCount;
            upButtons.get(index).setVisible(visible);
            downButtons.get(index).setVisible(visible);
            deleteButtons.get(index).setVisible(visible);
            upButtons.get(index).setEnabled(visible && index > 0);
            downButtons.get(index).setEnabled(visible && index < size - 1);
            deleteButtons.get(index).setEnabled(visible);
        }
    }

    private void moveIndex(int index, int direction) {
        int target = index + direction;
        List<String> commands = currentCommands();
        if (index < 0 || index >= commands.size() || target < 0 || target >= commands.size()) return;
        try {
            List<String> updated = new ArrayList<>(commands);
            String command = updated.remove(index);
            updated.add(target, command);
            persist(updated);
            if (target < scroll.smoothOffset()) scroll.setOffset(target);
            if (target >= scroll.smoothOffset() + VISIBLE_ROWS) scroll.setOffset(target - VISIBLE_ROWS + 1);
            updateRowButtons();
        } catch (Throwable throwable) {
            KineticOverlays.toast(null, text.saveFailedMessage(), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        deferredTooltip = null;
        updateRowButtons();
        graphics.fillGradient(0, 0, canvasWidth(), canvasHeight(), 0xFF171717, 0xFF0E0E0E);
        GuiTheme.panel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        GuiTheme.panelAlt(graphics, LIST_X - 4, LIST_Y - 4, LIST_W + 8, LIST_H + 8);
        graphics.drawCenteredString(font, title, canvasWidth() / 2, 30, 0xFFFFFF);

        renderRows(graphics, mouseX, mouseY);
        scroll.render(graphics, mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18);

        if (currentCommands().isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    text.emptyMessage(),
                    LIST_X + LIST_W / 2,
                    LIST_Y + LIST_H / 2,
                    0xFFFFFF
            );
        }
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> commands = currentCommands();
        scroll.update(commands.size(), VISIBLE_ROWS);
        double smoothOffset = scroll.smoothOffset();
        int first = Math.max(0, (int) Math.floor(smoothOffset));
        int end = Math.min(commands.size(), first + VISIBLE_ROWS + 2);
        int actionWidth = MOVE_W * 2 + DELETE_W + BUTTON_GAP * 2 + 12;

        enableUiScissor(graphics, LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H);
        try {
            for (int index = first; index < end; index++) {
                int y = LIST_Y + (int) Math.round((index - smoothOffset) * ROW_H);
                boolean hovered = mouseX >= LIST_X
                        && mouseX < LIST_X + LIST_W - actionWidth
                        && mouseY >= LIST_Y
                        && mouseY < LIST_Y + LIST_H
                        && mouseY >= y
                        && mouseY < y + ROW_H - 2;
                graphics.fill(LIST_X, y, LIST_X + LIST_W, y + ROW_H - 2, index % 2 == 0 ? 0xCC181818 : 0xCC111111);
                GuiTheme.stateOutline(graphics, LIST_X, y, LIST_W, ROW_H - 2, false, hovered, false);

                String display = displayCommand(commands.get(index));
                int commandWidth = LIST_W - actionWidth - 14;
                KineticText.drawScrollingLeft(
                        graphics,
                        font,
                        Component.literal(display),
                        LIST_X + 7,
                        y + 7,
                        commandWidth,
                        0xFFFFFF,
                        false
                );

                if (hovered) {
                    deferredTooltip = List.of(
                            Component.literal(display),
                            KineticText.translatable("gui.kineticcore.command_list.edit_hint")
                    );
                }
            }
        } finally {
            disableUiScissor(graphics);
        }
    }

    private String displayCommand(String command) {
        if (command == null) return "/";
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= LIST_X && mouseX < LIST_X + LIST_W
                && mouseY >= LIST_Y && mouseY < LIST_Y + LIST_H;
    }

    private int rowIndex(double mouseY) {
        if (mouseY < LIST_Y || mouseY >= LIST_Y + LIST_H) return -1;
        double contentY = mouseY - LIST_Y + scroll.smoothOffset() * ROW_H;
        int index = (int) Math.floor(contentY / ROW_H);
        return index >= 0 && index < currentCommands().size() ? index : -1;
    }

    private void openEditor(int index) {
        KineticClientRuntime.openScreen(new CommandEditorScreen(this, index));
    }

    private void deleteCommand(int index) {
        List<String> commands = currentCommands();
        if (index < 0 || index >= commands.size()) return;
        try {
            List<String> updated = new ArrayList<>(commands);
            updated.remove(index);
            persist(updated);
            updateScrollRange();
            updateRowButtons();
            KineticOverlays.toast(null, text.deletedMessage(), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        } catch (Throwable throwable) {
            KineticOverlays.toast(null, text.saveFailedMessage(), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
        }
    }

    private void persist(List<String> updated) {
        List<String> copy = new ArrayList<>(updated);
        if (!KTServerConfigClient.savePartial(serverPageId, Map.of(serverEntryId, copy))) {
            throw new IllegalStateException("Server config is not editable");
        }
        commandSetter.accept(copy);
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (super.canvasMouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && scroll.beginDrag(mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18, 2)) return true;
        if (button == 0 && inList(mouseX, mouseY)) {
            int index = rowIndex(mouseY);
            if (index >= 0) {
                openEditor(index);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean canvasMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scroll.drag(mouseY, LIST_Y, LIST_H, 18)
                || super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        return scroll.release(button) || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (inList(mouseX, mouseY) && scroll.scroll(delta, 1.0D)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        if (deferredTooltip != null) {
            showTooltip(deferredTooltip, null);
        }
    }

    private void closeToParent() {
        navigateBack();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
