package dev.xyat.kineticcore.feature.firstjoin.client;

import dev.xyat.kineticcore.api.client.AdaptiveItemGridRenderer;
import dev.xyat.kineticcore.api.client.GuiRenderUtil;
import dev.xyat.kineticcore.api.client.GuiToastUtil;
import dev.xyat.kineticcore.api.client.ItemCache;
import dev.xyat.kineticcore.api.client.ItemSelectorScreen;
import dev.xyat.kineticcore.api.client.ScaledScreen;
import dev.xyat.kineticcore.api.client.gui.ConfigScrollbarTheme;
import dev.xyat.kineticcore.api.client.gui.GridScrollController;
import dev.xyat.kineticcore.api.client.gui.NbtEditorScreen;
import dev.xyat.kineticcore.config.client.KTServerConfigClient;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FirstJoinRewardItemsScreen extends ScaledScreen {
    private static final int PANEL_X = 24;
    private static final int PANEL_Y = 18;
    private static final int PANEL_W = 592;
    private static final int PANEL_H = 324;
    private static final int LIST_X = 44;
    private static final int LIST_Y = 58;
    private static final int LIST_W = 538;
    private static final int ROW_H = 32;
    private static final int VISIBLE_ROWS = 7;
    private static final int LIST_H = ROW_H * VISIBLE_ROWS;
    private static final int COUNT_LABEL_X = LIST_X + 12;
    private static final int COUNT_FIELD_X = LIST_X + 52;
    private static final int COUNT_FIELD_W = 30;
    private static final int ITEM_X = LIST_X + 94;
    private static final int ITEM_Y_OFFSET = 7;
    private static final int SLOT_SIZE = 18;
    private static final int SCROLL_X = LIST_X + LIST_W + 6;
    private static final int SCROLL_W = 6;
    private static final int MOVE_BUTTON_W = 30;
    private static final int DELETE_BUTTON_W = 48;
    private static final int BUTTON_GAP = 3;

    private final Screen parent;
    private final List<RewardEntry> entries = new ArrayList<>();
    private final List<RewardEntry> savedEntries = new ArrayList<>();
    private final GridScrollController scroll = new GridScrollController();
    private final List<EditBox> countFields = new ArrayList<>();
    private final List<Button> upButtons = new ArrayList<>();
    private final List<Button> downButtons = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();
    private boolean updatingCountFields;
    private int hoveredIndex = -1;

    public FirstJoinRewardItemsScreen(Screen parent) {
        super(Component.translatable("gui.kineticcore.firstjoin.reward_items.title"));
        this.parent = parent;
        List<String> rawItems = KTServerConfigClient.getStringList(
                PlayerConfigGui.PAGE_ID,
                "items",
                PlayerConfig.firstJoinItemsRaw
        );
        int defaultSlot = 0;
        for (String raw : rawItems) {
            int slot = defaultSlot;
            String itemText = raw == null ? "" : raw.trim();
            if (itemText.startsWith("[")) {
                int end = itemText.indexOf(']');
                if (end > 1) {
                    try {
                        slot = Integer.parseInt(itemText.substring(1, end));
                        itemText = itemText.substring(end + 1).trim();
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            ItemStack stack = PlayerConfig.parseItemStack(itemText);
            if (!stack.isEmpty()) {
                RewardEntry loaded = new RewardEntry(slot, stack.copy());
                entries.add(loaded);
                savedEntries.add(copyEntry(loaded));
            }
            defaultSlot++;
        }
        configureResponsiveCanvas(640F, 360F, 6);
    }

    @Override
    protected void initScaled() {
        countFields.clear();
        upButtons.clear();
        downButtons.clear();
        deleteButtons.clear();
        updateScrollRange();

        int deleteX = LIST_X + LIST_W - DELETE_BUTTON_W - 4;
        int downX = deleteX - BUTTON_GAP - MOVE_BUTTON_W;
        int upX = downX - BUTTON_GAP - MOVE_BUTTON_W;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int localRow = row;
            int y = LIST_Y + row * ROW_H + 6;

            EditBox countField = new EditBox(
                    font,
                    COUNT_FIELD_X,
                    y,
                    COUNT_FIELD_W,
                    20,
                    Component.translatable("gui.kineticcore.firstjoin.reward_items.count")
            );
            countField.setMaxLength(3);
            countField.setFilter(value -> value.isEmpty() || value.matches("[1-9]\\d{0,2}"));
            countField.setResponder(value -> applyVisibleCount(localRow, value));
            countFields.add(addRenderableWidget(countField));

            upButtons.add(addRenderableWidget(Button.builder(
                            Component.literal("↑"),
                            button -> moveVisible(localRow, -1))
                    .bounds(upX, y, MOVE_BUTTON_W, 20)
                    .build()));
            downButtons.add(addRenderableWidget(Button.builder(
                            Component.literal("↓"),
                            button -> moveVisible(localRow, 1))
                    .bounds(downX, y, MOVE_BUTTON_W, 20)
                    .build()));
            deleteButtons.add(addRenderableWidget(Button.builder(
                            Component.translatable("gui.kineticcore.firstjoin.reward_items.delete"),
                            button -> deleteVisible(localRow))
                    .bounds(deleteX, y, DELETE_BUTTON_W, 20)
                    .build()));
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticcore.firstjoin.reward_items.add"),
                        button -> addEntry())
                .bounds(44, 314, 110, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticcore.firstjoin.reward_items.back"),
                        button -> requestClose())
                .bounds(265, 314, 110, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.kineticcore.firstjoin.reward_items.save"),
                        button -> saveAndClose())
                .bounds(472, 314, 110, 20)
                .build());
        updateRowButtons();
    }

    private void updateScrollRange() {
        scroll.update(entries.size(), VISIBLE_ROWS);
    }

    private int visibleIndex(int localRow) {
        int index = scroll.offset() + localRow;
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private void updateRowButtons() {
        updatingCountFields = true;
        try {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                int index = visibleIndex(row);
                boolean visible = index >= 0;
                boolean stackable = visible && entries.get(index).stack().getMaxStackSize() > 1;

                EditBox countField = countFields.get(row);
                countField.setVisible(stackable);
                countField.setEditable(stackable);
                if (stackable) {
                    String value = String.valueOf(entries.get(index).stack().getCount());
                    if (!countField.isFocused() && !value.equals(countField.getValue())) {
                        countField.setValue(value);
                    }
                } else {
                    countField.setFocused(false);
                    if (!countField.getValue().isEmpty()) {
                        countField.setValue("");
                    }
                }

                upButtons.get(row).visible = visible;
                downButtons.get(row).visible = visible;
                deleteButtons.get(row).visible = visible;
                if (visible) {
                    upButtons.get(row).active = index > 0;
                    downButtons.get(row).active = index < entries.size() - 1;
                    deleteButtons.get(row).active = true;
                }
            }
        } finally {
            updatingCountFields = false;
        }
    }

    private void applyVisibleCount(int localRow, String value) {
        if (updatingCountFields || value == null || value.isBlank()) return;
        int index = visibleIndex(localRow);
        if (index < 0) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.getMaxStackSize() <= 1) return;
        try {
            stack.setCount(Math.max(1, Math.min(999, Integer.parseInt(value))));
        } catch (NumberFormatException ignored) {
        }
    }

    private void moveVisible(int localRow, int direction) {
        clearCountFieldFocus();
        int index = visibleIndex(localRow);
        int target = index + direction;
        if (index < 0 || target < 0 || target >= entries.size()) return;
        RewardEntry entry = entries.remove(index);
        entries.add(target, entry);
        if (target < scroll.offset()) scroll.setOffset(target);
        if (target >= scroll.offset() + VISIBLE_ROWS) scroll.setOffset(target - VISIBLE_ROWS + 1);
        updateRowButtons();
    }

    private void deleteVisible(int localRow) {
        clearCountFieldFocus();
        int index = visibleIndex(localRow);
        if (index < 0) return;
        entries.remove(index);
        updateScrollRange();
        updateRowButtons();
    }

    private void clearCountFieldFocus() {
        for (EditBox countField : countFields) {
            countField.setFocused(false);
        }
    }

    private void addEntry() {
        clearCountFieldFocus();
        ItemCache.prepareCache(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
                if (selection == null || !selection.isItem()) return;
                entries.add(new RewardEntry(firstFreeInventorySlot(), selection.stack().copy()));
                updateScrollRange();
                int last = entries.size() - 1;
                if (last >= scroll.offset() + VISIBLE_ROWS) {
                    scroll.setOffset(last - VISIBLE_ROWS + 1);
                }
                updateRowButtons();
            }));
        });
    }

    private int firstFreeInventorySlot() {
        for (int slot = 0; slot < 36; slot++) {
            boolean used = false;
            for (RewardEntry entry : entries) {
                if (entry.slot() == slot) {
                    used = true;
                    break;
                }
            }
            if (!used) return slot;
        }
        return entries.size();
    }

    private void openItemSelector(int index) {
        clearCountFieldFocus();
        if (index < 0 || index >= entries.size()) return;
        ItemCache.prepareCache(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ItemSelectorScreen(this, selection -> {
                if (selection == null || !selection.isItem() || index >= entries.size()) return;
                ItemStack selected = selection.stack().copy();
                int oldCount = entries.get(index).stack().getCount();
                if (selected.getMaxStackSize() > 1) {
                    selected.setCount(Math.max(1, Math.min(999, oldCount)));
                } else {
                    selected.setCount(1);
                }
                entries.set(index, new RewardEntry(entries.get(index).slot(), selected));
                updateRowButtons();
            }));
        });
    }

    private void openNbtEditor(int index) {
        clearCountFieldFocus();
        if (index < 0 || index >= entries.size()) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.isEmpty()) return;
        String initialNbt = stack.hasTag() && stack.getTag() != null ? stack.getTag().toString() : "";
        Minecraft.getInstance().setScreen(new NbtEditorScreen(initialNbt, value -> {
            if (value == null || value.isBlank()) {
                stack.setTag(null);
                return;
            }
            try {
                stack.setTag(TagParser.parseTag(value));
            } catch (Exception ignored) {
            }
        }, this));
    }

    private void saveAndClose() {
        clearCountFieldFocus();
        List<String> saved = new ArrayList<>();
        for (RewardEntry entry : entries) {
            if (!entry.stack().isEmpty()) {
                saved.add("[" + entry.slot() + "] " + PlayerConfig.serializeItemStack(entry.stack()));
            }
        }
        if (!KTServerConfigClient.savePartial(PlayerConfigGui.PAGE_ID, Map.of("items", saved))) {
            GuiToastUtil.showToast(Component.translatable("gui.kineticcore.config.server.save_failed"));
            return;
        }
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void requestClose() {
        clearCountFieldFocus();
        Minecraft client = Minecraft.getInstance();
        if (!hasUnsavedChanges()) {
            client.setScreen(parent);
            return;
        }

        client.setScreen(new ConfirmScreen(
                shouldSave -> {
                    if (!shouldSave) {
                        client.setScreen(parent);
                        return;
                    }
                    client.setScreen(this);
                    saveAndClose();
                },
                Component.translatable("gui.kineticcore.config.unsaved_action.title"),
                Component.translatable("gui.kineticcore.firstjoin.reward_items.unsaved")
        ));
    }

    private boolean hasUnsavedChanges() {
        if (entries.size() != savedEntries.size()) return true;
        for (int i = 0; i < entries.size(); i++) {
            RewardEntry current = entries.get(i);
            RewardEntry saved = savedEntries.get(i);
            if (current.slot() != saved.slot()) return true;
            if (!sameStack(current.stack(), saved.stack())) return true;
        }
        return false;
    }

    private static boolean sameStack(ItemStack left, ItemStack right) {
        CompoundTag leftTag = new CompoundTag();
        CompoundTag rightTag = new CompoundTag();
        left.save(leftTag);
        right.save(rightTag);
        return leftTag.equals(rightTag);
    }

    private static RewardEntry copyEntry(RewardEntry entry) {
        return new RewardEntry(entry.slot(), entry.stack().copy());
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

    private boolean overItem(double mouseX, double mouseY, int index) {
        if (index < 0 || index >= entries.size()) return false;
        int localRow = index - scroll.offset();
        if (localRow < 0 || localRow >= VISIBLE_ROWS) return false;
        int y = LIST_Y + localRow * ROW_H + ITEM_Y_OFFSET;
        return GuiRenderUtil.isHovering(mouseX, mouseY, ITEM_X, y, SLOT_SIZE, SLOT_SIZE);
    }

    @Override
    protected void renderScaledBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoveredIndex = rowIndex(mouseY);
        updateRowButtons();
        graphics.fillGradient(0, 0, vWidth, vHeight, 0xFF171717, 0xFF0E0E0E);
        GuiRenderUtil.drawStandardPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        GuiRenderUtil.drawDarkPanel(graphics, LIST_X - 4, LIST_Y - 4, LIST_W + 8, LIST_H + 8);
        graphics.drawCenteredString(font, title, vWidth / 2, 30, 0xFFFFFF);

        int start = scroll.offset();
        int end = Math.min(start + VISIBLE_ROWS, entries.size());
        for (int index = start; index < end; index++) {
            int local = index - start;
            int y = LIST_Y + local * ROW_H;
            boolean rowHovered = index == hoveredIndex;
            graphics.fill(LIST_X, y, LIST_X + LIST_W, y + ROW_H - 2, local % 2 == 0 ? 0xCC181818 : 0xCC111111);
            graphics.renderOutline(LIST_X, y, LIST_W, ROW_H - 2, rowHovered ? 0xFF55AAFF : 0xFF555555);

            RewardEntry entry = entries.get(index);
            ItemStack stack = entry.stack();
            int itemY = y + ITEM_Y_OFFSET;
            boolean itemHovered = overItem(mouseX, mouseY, index);
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                graphics.drawString(
                        font,
                        Component.translatable("gui.kineticcore.firstjoin.reward_items.count"),
                        COUNT_LABEL_X,
                        y + 12,
                        0xFFFFFFFF,
                        false
                );
            }

            AdaptiveItemGridRenderer.drawSlot(graphics, ITEM_X, itemY, SLOT_SIZE, 4, itemHovered);
            AdaptiveItemGridRenderer.renderItem(graphics, font, stack, ITEM_X, itemY, SLOT_SIZE, 1.0F, false);

            graphics.drawString(
                    font,
                    GuiRenderUtil.trimText(font, stack.getHoverName().getString(), 220),
                    ITEM_X + SLOT_SIZE + 8,
                    y + 11,
                    0xFFFFFFFF,
                    false
            );
        }

        ConfigScrollbarTheme.render(scroll, graphics, mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18);
        if (entries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.kineticcore.firstjoin.reward_items.empty"),
                    LIST_X + LIST_W / 2,
                    LIST_Y + LIST_H / 2,
                    0xFFFFFFFF
            );
        }
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.kineticcore.firstjoin.reward_items.hint"),
                vWidth / 2,
                292,
                0xFFFFFFFF
        );
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int scaledMouseX, int scaledMouseY, int mouseX, int mouseY) {
        int index = rowIndex(scaledMouseY);
        if (!overItem(scaledMouseX, scaledMouseY, index)) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(mouseX, mouseY, 500);
        graphics.pose().scale(guiScale, guiScale, 1.0F);
        graphics.pose().translate(-mouseX, -mouseY, 0);
        graphics.renderTooltip(font, stack, mouseX, mouseY);
        graphics.pose().popPose();
    }

    @Override
    protected boolean universalMouseClicked(double mouseX, double mouseY, int button) {
        if (super.universalMouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && scroll.beginDrag(mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18, 2)) return true;
        if (inList(mouseX, mouseY)) {
            int index = rowIndex(mouseY);
            if (overItem(mouseX, mouseY, index)) {
                if (button == 0) {
                    openItemSelector(index);
                    return true;
                }
                if (button == 1) {
                    openNbtEditor(index);
                    return true;
                }
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
            clearCountFieldFocus();
            updateRowButtons();
            return true;
        }
        return super.universalMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        requestClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record RewardEntry(int slot, ItemStack stack) {
    }
}
