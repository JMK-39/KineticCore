package dev.xyat.kineticcore.feature.firstjoin.client;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;


import dev.xyat.kineticcore.api.text.KineticI18n;
import dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors;

import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.overlay.KineticOverlays;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.ui.KineticUi;
import dev.xyat.kineticcore.api.client.gui.ui.NumberType;
import dev.xyat.kineticcore.api.client.gui.widget.KineticButton;
import dev.xyat.kineticcore.api.client.gui.widget.KineticNumberField;
import dev.xyat.kineticcore.api.config.client.KTServerConfigClient;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfig;
import dev.xyat.kineticcore.feature.firstjoin.config.PlayerConfigGui;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FirstJoinRewardItemsPage extends KineticPage {
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
    private static final int SCROLL_W = 4;
    private static final int MOVE_BUTTON_W = 30;
    private static final int DELETE_BUTTON_W = 48;
    private static final int BUTTON_GAP = 3;

    private final List<RewardEntry> entries = new ArrayList<>();
    private final List<RewardEntry> savedEntries = new ArrayList<>();
    private final KineticScrollController scroll = new KineticScrollController();
    private final List<KineticNumberField> countFields = new ArrayList<>();
    private final List<KineticButton> upButtons = new ArrayList<>();
    private final List<KineticButton> downButtons = new ArrayList<>();
    private final List<KineticButton> deleteButtons = new ArrayList<>();
    private boolean updatingCountFields;

    public FirstJoinRewardItemsPage() {
        super(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.title"));
        setPausesGame(false);
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
    }

    @Override
    protected void build(KineticUi ui) {
        countFields.clear();
        upButtons.clear();
        downButtons.clear();
        deleteButtons.clear();
        updateScrollRange();

        int deleteX = LIST_X + LIST_W - DELETE_BUTTON_W - 4;
        int downX = deleteX - BUTTON_GAP - MOVE_BUTTON_W;
        int upX = downX - BUTTON_GAP - MOVE_BUTTON_W;
        KineticUi list = ui.scrollViewport(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H,
                () -> scroll.smoothOffset() * ROW_H);

        for (int index = 0; index < entries.size(); index++) {
            final int entryIndex = index;
            int y = LIST_Y + index * ROW_H + 6;

            KineticNumberField countField = list.numberField(COUNT_FIELD_X, y, COUNT_FIELD_W, NumberType.INT)
                    .label(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.count"))
                    .allowNegative(false)
                    .range(1, 999)
                    .build();
            countField.setMaxLength(3);
            countField.setResponder(value -> applyCount(entryIndex, value));
            countFields.add(countField);

            upButtons.add(list.button(upX, y, MOVE_BUTTON_W)
                    .text(KineticI18n.translatable("gui.kineticcore.symbol.up"))
                    .onClick(() -> moveIndex(entryIndex, -1))
                    .build());
            downButtons.add(list.button(downX, y, MOVE_BUTTON_W)
                    .text(KineticI18n.translatable("gui.kineticcore.symbol.down"))
                    .onClick(() -> moveIndex(entryIndex, 1))
                    .build());
            deleteButtons.add(list.button(deleteX, y, DELETE_BUTTON_W)
                    .text(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.delete"))
                    .onClick(() -> deleteIndex(entryIndex))
                    .build());
        }

        ui.button(44, 314, 110)
                .text(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.add"))
                .onClick(this::addEntry)
                .build();
        ui.button(265, 314, 110)
                .text(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.back"))
                .onClick(this::requestClose)
                .build();
        ui.button(472, 314, 110)
                .text(KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.save"))
                .onClick(() -> save())
                .build();
        updateRowButtons();
    }

    private void updateScrollRange() {
        scroll.update(entries.size(), VISIBLE_ROWS);
    }

    private void updateRowButtons() {
        updatingCountFields = true;
        try {
            int widgetCount = Math.min(entries.size(), countFields.size());
            for (int index = 0; index < countFields.size(); index++) {
                boolean visible = index < widgetCount;
                boolean stackable = visible && entries.get(index).stack().getMaxStackSize() > 1;

                KineticNumberField countField = countFields.get(index);
                countField.setVisible(stackable);
                countField.setTextEditable(stackable);
                if (stackable) {
                    String value = String.valueOf(entries.get(index).stack().getCount());
                    if (!isFocused(countField) && !value.equals(countField.getValue())) {
                        countField.setValue(value);
                    }
                } else {
                    blur(countField);
                    if (!countField.getValue().isEmpty()) {
                        countField.setValue("");
                    }
                }

                upButtons.get(index).setVisible(visible);
                downButtons.get(index).setVisible(visible);
                deleteButtons.get(index).setVisible(visible);
                if (visible) {
                    upButtons.get(index).setEnabled(index > 0);
                    downButtons.get(index).setEnabled(index < entries.size() - 1);
                    deleteButtons.get(index).setEnabled(true);
                }
            }
        } finally {
            updatingCountFields = false;
        }
    }

    private void applyCount(int index, String value) {
        if (updatingCountFields || value == null || value.isBlank()) return;
        if (index < 0 || index >= entries.size()) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.getMaxStackSize() <= 1) return;
        try {
            stack.setCount(Math.max(1, Math.min(999, Integer.parseInt(value))));
        } catch (NumberFormatException ignored) {
        }
    }

    private void moveIndex(int index, int direction) {
        clearCountFieldFocus();
        int target = index + direction;
        if (index < 0 || index >= entries.size() || target < 0 || target >= entries.size()) return;
        RewardEntry entry = entries.remove(index);
        entries.add(target, entry);
        if (target < scroll.smoothOffset()) scroll.setOffset(target);
        if (target >= scroll.smoothOffset() + VISIBLE_ROWS) scroll.setOffset(target - VISIBLE_ROWS + 1);
        updateRowButtons();
    }

    private void deleteIndex(int index) {
        clearCountFieldFocus();
        if (index < 0 || index >= entries.size()) return;
        entries.remove(index);
        updateScrollRange();
        rebuild();
    }

    private void clearCountFieldFocus() {
        for (KineticNumberField countField : countFields) {
            blur(countField);
        }
    }

    private void addEntry() {
        clearCountFieldFocus();
        KineticSelectors.openItemSelector(selection -> {
                if (selection == null || !selection.isItem()) return;
                entries.add(new RewardEntry(firstFreeInventorySlot(), selection.stack().copy()));
                updateScrollRange();
                int last = entries.size() - 1;
                if (last >= scroll.smoothOffset() + VISIBLE_ROWS) {
                    scroll.setOffset(last - VISIBLE_ROWS + 1);
                }
                updateRowButtons();
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
        KineticSelectors.openItemSelector(selection -> {
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
        });
    }

    private void openNbtEditor(int index) {
        clearCountFieldFocus();
        if (index < 0 || index >= entries.size()) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.isEmpty()) return;
        String initialNbt = stack.hasTag() && stack.getTag() != null ? stack.getTag().toString() : "";
        KineticSelectors.openNbtEditor(initialNbt, value -> {
            if (value == null || value.isBlank()) {
                stack.setTag(null);
                return;
            }
            try {
                stack.setTag(TagParser.parseTag(value));
            } catch (Exception ignored) {
            }
        });
    }

    private boolean save() {
        clearCountFieldFocus();
        List<String> saved = new ArrayList<>();
        for (RewardEntry entry : entries) {
            if (!entry.stack().isEmpty()) {
                saved.add("[" + entry.slot() + "] " + PlayerConfig.serializeItemStack(entry.stack()));
            }
        }
        if (!KTServerConfigClient.savePartial(PlayerConfigGui.PAGE_ID, Map.of("items", saved))) {
            KineticOverlays.toast(null, KineticI18n.translatable("gui.kineticcore.config.server.save_failed"), KineticOverlays.Position.BOTTOM_CENTER, 5000, 0, -30);
            return false;
        }
        savedEntries.clear();
        for (RewardEntry entry : entries) {
            savedEntries.add(copyEntry(entry));
        }
        return true;
    }

    private void saveAndClose() {
        if (save()) {
            navigateBack();
        }
    }

    private void requestClose() {
        clearCountFieldFocus();
        if (hasNoUnsavedChanges()) {
            navigateBack();
            return;
        }

        openDialog(
                KineticI18n.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.unsaved"),
                KineticI18n.translatable("gui.yes"),
                KineticI18n.translatable("gui.no"),
                this::saveAndClose,
                () -> navigateBack()
        );
    }

    private boolean hasNoUnsavedChanges() {
        if (entries.size() != savedEntries.size()) return false;
        for (int i = 0; i < entries.size(); i++) {
            RewardEntry current = entries.get(i);
            RewardEntry saved = savedEntries.get(i);
            if (current.slot() != saved.slot()) return false;
            if (stacksDiffer(current.stack(), saved.stack())) return false;
        }
        return true;
    }

    private static boolean stacksDiffer(ItemStack left, ItemStack right) {
        CompoundTag leftTag = new CompoundTag();
        CompoundTag rightTag = new CompoundTag();
        left.save(leftTag);
        right.save(rightTag);
        return !leftTag.equals(rightTag);
    }

    private static RewardEntry copyEntry(RewardEntry entry) {
        return new RewardEntry(entry.slot(), entry.stack().copy());
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= LIST_X && mouseX < LIST_X + LIST_W
                && mouseY >= LIST_Y && mouseY < LIST_Y + LIST_H;
    }

    private int rowIndex(double mouseY) {
        if (mouseY < LIST_Y || mouseY >= LIST_Y + LIST_H) return -1;
        double contentY = mouseY - LIST_Y + scroll.smoothOffset() * ROW_H;
        int index = (int) Math.floor(contentY / ROW_H);
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private boolean overItem(double mouseX, double mouseY, int index) {
        if (index < 0 || index >= entries.size() || !inList(mouseX, mouseY)) return false;
        int y = LIST_Y + (int) Math.round((index - scroll.smoothOffset()) * ROW_H) + ITEM_Y_OFFSET;
        return KineticTheme.hovering(mouseX, mouseY, ITEM_X, y, SLOT_SIZE, SLOT_SIZE);
    }

    @Override
    protected void renderBackground(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int hoveredIndex = rowIndex(mouseY);
        updateRowButtons();
        KineticTheme.canvasBackground(graphics, width(), height());
        KineticTheme.panel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        KineticTheme.panelAlt(graphics, LIST_X - 4, LIST_Y - 4, LIST_W + 8, LIST_H + 8);
        graphics.centeredText(title(), width() / 2, 30, KineticTheme.current().text(), true);

        scroll.update(entries.size(), VISIBLE_ROWS);
        double smoothOffset = scroll.smoothOffset();
        int first = Math.max(0, (int) Math.floor(smoothOffset));
        int end = Math.min(entries.size(), first + VISIBLE_ROWS + 2);
        graphics.scissor(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + LIST_H);
        try {
            for (int index = first; index < end; index++) {
                int y = LIST_Y + (int) Math.round((index - smoothOffset) * ROW_H);
                boolean rowHovered = index == hoveredIndex;
                KineticTheme.stateSurface(
                        graphics,
                        LIST_X,
                        y,
                        LIST_W,
                        ROW_H - 2,
                        index % 2 == 0 ? KineticTheme.Surface.PANEL_ALT : KineticTheme.Surface.PANEL,
                        false,
                        rowHovered,
                        false
                );

                RewardEntry entry = entries.get(index);
                ItemStack stack = entry.stack();
                int itemY = y + ITEM_Y_OFFSET;
                boolean itemHovered = overItem(mouseX, mouseY, index);
                if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                    graphics.text(
                            KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.count"),
                            COUNT_LABEL_X,
                            y + 12,
                            KineticTheme.current().text(),
                            false
                    );
                }

                KineticTheme.itemSlot(graphics, ITEM_X, itemY, SLOT_SIZE, SLOT_SIZE, 4, false, itemHovered, false);
                KineticTheme.item(graphics, stack, ITEM_X, itemY, SLOT_SIZE, 1.0F, false);

                graphics.scrollingText(
                        stack.getHoverName(),
                        ITEM_X + SLOT_SIZE + 8,
                        y + 11,
                        220,
                        KineticTheme.current().text(),
                        false
                );
            }
        } finally {
            graphics.endScissor();
        }

        scroll.render(graphics, mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18);
        if (entries.isEmpty()) {
            graphics.centeredText(
                    KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.empty"),
                    LIST_X + LIST_W / 2,
                    LIST_Y + LIST_H / 2,
                    KineticTheme.current().text(),
                    true
            );
        }
        graphics.centeredText(
                KineticI18n.translatable("gui.kineticcore.firstjoin.reward_items.hint"),
                width() / 2,
                292,
                KineticTheme.current().text(),
                true
        );
    }

    @Override
    protected void renderTooltips(int mouseX, int mouseY) {
        int index = rowIndex(mouseY);
        if (!overItem(mouseX, mouseY, index)) return;
        ItemStack stack = entries.get(index).stack();
        if (stack.isEmpty()) return;
        showItemTooltip(stack);
    }

    @Override
    protected boolean onMouseClick(MouseInput input) {
        double mouseX = input.x();
        double mouseY = input.y();
        if (scroll.beginDrag(mouseX, mouseY, input.button(), SCROLL_X, LIST_Y, SCROLL_W, LIST_H, 18)) return true;
        if (inList(mouseX, mouseY)) {
            int index = rowIndex(mouseY);
            if (overItem(mouseX, mouseY, index)) {
                if (input.isLeft()) {
                    openItemSelector(index);
                    return true;
                }
                if (input.isRight()) {
                    openNbtEditor(index);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseDrag(MouseDragInput input) {
        return scroll.drag(input.y(), LIST_Y, LIST_H, 18);
    }

    @Override
    protected boolean onMouseRelease(MouseInput input) {
        return scroll.release(input.button());
    }

    @Override
    protected boolean onMouseScroll(ScrollInput input) {
        if (inList(input.x(), input.y()) && scroll.scroll(input.deltaY(), 1.0D)) {
            clearCountFieldFocus();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onCloseRequested() {
        requestClose();
        return true;
    }

    private record RewardEntry(int slot, ItemStack stack) {
    }
}
