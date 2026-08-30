package dev.xyat.kineticcore.feature.firstjoin.client;

import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.ConfigScrollbarTheme;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import dev.xyat.kineticcore.config.client.KTServerConfigClient;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfigGui;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfigGui;
import dev.xyat.kineticcore.feature.worldinit.config.WorldInitConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
public final class FirstJoinCommandListScreen extends ScaledScreen {
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
    private static final int SCROLL_W = 6;
    private static final int MOVE_W = 34;
    private static final int DELETE_W = 54;
    private static final int BUTTON_GAP = 3;

    private final Screen parent;
    private final Supplier<List<String>> commandGetter;
    private final Consumer<List<String>> commandSetter;
    private final String serverPageId;
    private final String serverEntryId;
    private final Component addEditorTitle;
    private final Component editEditorTitle;
    private final Component emptyMessage;
    private final Component savedMessage;
    private final Component deletedMessage;
    private final Component saveFailedMessage;
    private final boolean showPlayerVariables;
    private final GridScrollController scroll = new GridScrollController();
    private final List<Button> upButtons = new ArrayList<>();
    private final List<Button> downButtons = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();
    private List<Component> deferredTooltip;

    public FirstJoinCommandListScreen(Screen parent) {
        this(
                parent,
                Component.translatable("gui.kineticcore.firstjoin.command_list.title"),
                Component.translatable("gui.kineticcore.firstjoin.command_edit.add_title"),
                Component.translatable("gui.kineticcore.firstjoin.command_edit.edit_title"),
                Component.translatable("gui.kineticcore.firstjoin.command_list.empty"),
                Component.translatable("msg.kineticcore.firstjoin.command_edit.saved"),
                Component.translatable("msg.kineticcore.firstjoin.command_list.deleted"),
                Component.translatable("msg.kineticcore.firstjoin.command_list.save_failed"),
                () -> PlayerConfig.firstJoinCommands,
                value -> PlayerConfig.firstJoinCommands = value,
                PlayerConfigGui.PAGE_ID,
                "commands",
                true
        );
    }

    public static FirstJoinCommandListScreen forWorldInit(Screen parent) {
        return new FirstJoinCommandListScreen(
                parent,
                Component.translatable("gui.kineticcore.worldinit.command_list.title"),
                Component.translatable("gui.kineticcore.worldinit.command_edit.add_title"),
                Component.translatable("gui.kineticcore.worldinit.command_edit.edit_title"),
                Component.translatable("gui.kineticcore.worldinit.command_list.empty"),
                Component.translatable("msg.kineticcore.worldinit.command_edit.saved"),
                Component.translatable("msg.kineticcore.worldinit.command_list.deleted"),
                Component.translatable("msg.kineticcore.worldinit.command_list.save_failed"),
                () -> WorldInitConfig.worldInitCommands,
                value -> WorldInitConfig.worldInitCommands = value,
                WorldInitConfigGui.PAGE_ID,
                "commands",
                false
        );
    }

    private FirstJoinCommandListScreen(
            Screen parent,
            Component title,
            Component addEditorTitle,
            Component editEditorTitle,
            Component emptyMessage,
            Component savedMessage,
            Component deletedMessage,
            Component saveFailedMessage,
            Supplier<List<String>> commandGetter,
            Consumer<List<String>> commandSetter,
            String serverPageId,
            String serverEntryId,
            boolean showPlayerVariables
    ) {
        super(title);
        this.parent = parent;
        this.addEditorTitle = addEditorTitle;
        this.editEditorTitle = editEditorTitle;
        this.emptyMessage = emptyMessage;
        this.savedMessage = savedMessage;
        this.deletedMessage = deletedMessage;
        this.saveFailedMessage = saveFailedMessage;
        this.commandGetter = commandGetter;
        this.commandSetter = commandSetter;
        this.serverPageId = serverPageId;
        this.serverEntryId = serverEntryId;
        this.commandSetter.accept(KTServerConfigClient.getStringList(serverPageId, serverEntryId, commandGetter.get()));
        this.showPlayerVariables = showPlayerVariables;
        configureResponsiveCanvas(640F, 360F, 6);
    }

    @Override
    protected void initScaled() {
        upButtons.clear();
        downButtons.clear();
        deleteButtons.clear();
        updateScrollRange();

        int deleteX = LIST_X + LIST_W - DELETE_W - 4;
        int downX = deleteX - BUTTON_GAP - MOVE_W;
        int upX = downX - BUTTON_GAP - MOVE_W;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int localRow = row;
            int y = LIST_Y + row * ROW_H + 2;
            upButtons.add(addRenderableWidget(Button.builder(
                            Component.literal("↑"),
                            button -> moveVisible(localRow, -1))
                    .bounds(upX, y, MOVE_W, ROW_H - 6)
                    .build()));
            downButtons.add(addRenderableWidget(Button.builder(
                            Component.literal("↓"),
                            button -> moveVisible(localRow, 1))
                    .bounds(downX, y, MOVE_W, ROW_H - 6)
                    .build()));
            deleteButtons.add(addRenderableWidget(Button.builder(
                            Component.translatable("gui.kineticcore.firstjoin.command_list.delete"),
                            button -> deleteVisible(localRow))
                    .bounds(deleteX, y, DELETE_W, ROW_H - 6)
                    .build()));
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticcore.firstjoin.command_list.add"),
                        button -> openEditor(-1))
                .bounds(44, 314, 110, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticcore.firstjoin.command_list.back"),
                        button -> closeToParent())
                .bounds(472, 314, 110, 20)
                .build());
        updateRowButtons();
    }

    List<String> currentCommands() {
        List<String> commands = commandGetter.get();
        return commands == null ? List.of() : commands;
    }

    Component editorTitle(int editingIndex) {
        return editingIndex >= 0 ? editEditorTitle : addEditorTitle;
    }

    boolean showPlayerVariables() {
        return showPlayerVariables;
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
        GuiToastUtil.showToast(savedMessage);
    }

    void refreshAfterEdit() {
        updateScrollRange();
        int lastIndex = Math.max(0, currentCommands().size() - 1);
        if (lastIndex >= scroll.offset() + VISIBLE_ROWS) {
            scroll.setOffset(lastIndex - VISIBLE_ROWS + 1);
        }
        updateRowButtons();
    }

    Component saveFailedMessage() {
        return saveFailedMessage;
    }

    private void updateScrollRange() {
        scroll.update(currentCommands().size(), VISIBLE_ROWS);
    }

    private int visibleIndex(int localRow) {
        int index = scroll.offset() + localRow;
        return index >= 0 && index < currentCommands().size() ? index : -1;
    }

    private void updateRowButtons() {
        if (upButtons.size() < VISIBLE_ROWS || downButtons.size() < VISIBLE_ROWS || deleteButtons.size() < VISIBLE_ROWS) {
            return;
        }
        int size = currentCommands().size();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = visibleIndex(row);
            boolean visible = index >= 0;
            upButtons.get(row).visible = visible;
            downButtons.get(row).visible = visible;
            deleteButtons.get(row).visible = visible;
            if (visible) {
                upButtons.get(row).active = index > 0;
                downButtons.get(row).active = index < size - 1;
                deleteButtons.get(row).active = true;
            }
        }
    }

    private void moveVisible(int localRow, int direction) {
        int index = visibleIndex(localRow);
        int target = index + direction;
        List<String> commands = currentCommands();
        if (index < 0 || target < 0 || target >= commands.size()) return;
        try {
            List<String> updated = new ArrayList<>(commands);
            String command = updated.remove(index);
            updated.add(target, command);
            persist(updated);
            if (target < scroll.offset()) scroll.setOffset(target);
            if (target >= scroll.offset() + VISIBLE_ROWS) scroll.setOffset(target - VISIBLE_ROWS + 1);
            updateRowButtons();
        } catch (Throwable throwable) {
            GuiToastUtil.showToast(saveFailedMessage);
        }
    }

    private void deleteVisible(int localRow) {
        int index = visibleIndex(localRow);
        if (index >= 0) deleteCommand(index);
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        deferredTooltip = null;
        updateRowButtons();
        graphics.fillGradient(0, 0, vWidth, vHeight, 0xFF171717, 0xFF0E0E0E);
        GuiRenderUtil.drawStandardPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        GuiRenderUtil.drawDarkPanel(graphics, LIST_X - 4, LIST_Y - 4, LIST_W + 8, LIST_H + 8);
        graphics.drawCenteredString(font, title, vWidth / 2, 30, 0xFFFFFF);

        renderRows(graphics, mouseX, mouseY);
        ConfigScrollbarTheme.render(scroll, graphics, mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18);

        if (currentCommands().isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    emptyMessage,
                    LIST_X + LIST_W / 2,
                    LIST_Y + LIST_H / 2,
                    0xFFFFFF
            );
        }
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> commands = currentCommands();
        int start = scroll.offset();
        int end = Math.min(start + VISIBLE_ROWS, commands.size());
        int actionWidth = MOVE_W * 2 + DELETE_W + BUTTON_GAP * 2 + 12;

        for (int index = start; index < end; index++) {
            int local = index - start;
            int y = LIST_Y + local * ROW_H;
            boolean hovered = mouseX >= LIST_X && mouseX < LIST_X + LIST_W - actionWidth
                    && mouseY >= y && mouseY < y + ROW_H - 2;
            graphics.fill(LIST_X, y, LIST_X + LIST_W, y + ROW_H - 2, local % 2 == 0 ? 0xCC181818 : 0xCC111111);
            graphics.renderOutline(LIST_X, y, LIST_W, ROW_H - 2, hovered ? 0xFF55AAFF : 0xFF555555);

            String display = displayCommand(commands.get(index));
            int commandWidth = LIST_W - actionWidth - 14;
            graphics.drawString(
                    font,
                    GuiRenderUtil.trimText(font, display, commandWidth),
                    LIST_X + 7,
                    y + 7,
                    0xFFFFFF,
                    false
            );

            if (hovered) {
                deferredTooltip = List.of(
                        Component.literal(display),
                        Component.translatable("gui.kineticcore.firstjoin.command_list.edit_hint")
                );
            }
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
        int localRow = (int) ((mouseY - LIST_Y) / ROW_H);
        if (localRow < 0 || localRow >= VISIBLE_ROWS) return -1;
        return visibleIndex(localRow);
    }

    private void openEditor(int index) {
        if (minecraft == null) return;
        minecraft.setScreen(new FirstJoinCommandEditScreen(this, index));
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
            GuiToastUtil.showToast(deletedMessage);
        } catch (Throwable throwable) {
            GuiToastUtil.showToast(saveFailedMessage);
        }
    }

    private void persist(List<String> updated) {
        List<String> copy = new ArrayList<>(updated);
        commandSetter.accept(copy);
        if (!KTServerConfigClient.savePartial(serverPageId, Map.of(serverEntryId, copy))) {
            throw new IllegalStateException("Server config is not editable");
        }
    }

    @Override
    protected boolean universalMouseClicked(double mouseX, double mouseY, int button) {
        if (super.universalMouseClicked(mouseX, mouseY, button)) return true;
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
    protected boolean universalMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return scroll.drag(mouseY, LIST_Y, LIST_H, 18)
                || super.universalMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean universalMouseReleased(double mouseX, double mouseY, int button) {
        return scroll.release(button) || super.universalMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean universalMouseScrolled(double mouseX, double mouseY, double delta) {
        if (inList(mouseX, mouseY) && scroll.scroll(delta)) {
            updateRowButtons();
            return true;
        }
        return super.universalMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        if (deferredTooltip != null) {
            graphics.renderComponentTooltip(font, deferredTooltip, mouseX, mouseY);
        }
    }

    private void closeToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
