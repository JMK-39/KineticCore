package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemActionList;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Smooth scrolling list control behind the public {@code Kinetic*List} API; created only through {@code KineticWidgets}. */
public final class ItemActionListWidget extends VerticalScrollListWidget implements KineticItemActionList {
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_PITCH = ROW_HEIGHT + 2;
    private static final int ROW_TOP_PADDING = 1;
    private static final int ITEM_SLOT_SIZE = 18;
    private static final int ITEM_LEFT_PADDING = 4;
    private static final int TEXT_LEFT_PADDING = 26;
    private static final int ACTION_GAP = 4;

    private final Font font;
    private final Consumer<Integer> responder;
    private final Consumer<Integer> actionResponder;
    private final int actionWidth;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private final List<StateButton> actionButtons = new ArrayList<>();
    private List<ItemActionItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private boolean hoveredAction;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public ItemActionListWidget(
            FactoryAccess access,
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int actionWidth,
            Consumer<Integer> responder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.font = font;
        this.responder = responder;
        this.actionResponder = actionResponder;
        this.actionWidth = Math.max(1, actionWidth);
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends ItemActionItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        actionButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            ItemActionItem item = items.get(index);
            int rowIndex = index;
            StateButton rowButton = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    rowWidth(),
                    ROW_HEIGHT,
                    Component.empty(),
                    ignored -> select(rowIndex, true)
            );
            rowButton.active = item != null && item.active();
            rowButton.setError(item != null && item.error());
            rowButtons.add(rowButton);

            StateButton actionButton = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    this.actionWidth,
                    KineticScreen.STANDARD_CONTROL_HEIGHT,
                    item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel(),
                    ignored -> {
                        ItemActionItem current = rowIndex < items.size() ? items.get(rowIndex) : null;
                        if (current == null || !current.actionActive()) return;
                        if (actionResponder != null) actionResponder.accept(rowIndex);
                    }
            );
            actionButton.active = item != null && item.actionActive();
            actionButtons.add(actionButton);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshSelection();
        refreshLayout();
    }

    @Override
    public List<ItemActionItem> items() {
        return List.copyOf(items);
    }

    @Override
    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    public void setSelectedIndex(int index) {
        if (items.isEmpty() || index < 0) {
            selectedIndex = -1;
            refreshSelection();
            return;
        }
        select(Math.min(items.size() - 1, index), false);
    }

    @Override
    public void ensureSelectedVisible() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return;
        int visible = visibleRows();
        int offset = scroll.offset();
        if (selectedIndex < offset) {
            setScrollOffset(selectedIndex);
        } else if (selectedIndex >= offset + visible) {
            setScrollOffset(selectedIndex - visible + 1);
        }
    }

    @Override
    public int itemAt(double mouseX, double mouseY) {
        return rowIndexAt(mouseX, mouseY, false);
    }

    @Override
    public int actionAt(double mouseX, double mouseY) {
        return rowIndexAt(mouseX, mouseY, true);
    }

    @Override
    public ItemStack stackAt(double mouseX, double mouseY) {
        int index = itemAt(mouseX, mouseY);
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        StateButton button = rowButtons.get(index);
        int slotX = button.getX() + ITEM_LEFT_PADDING;
        int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
        if (!GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) return ItemStack.EMPTY;
        return safeStack(items.get(index));
    }

    @Override
    public ItemStack hoveredStack() {
        return hoveredStack;
    }

    @Override
    protected Component hoveredRowTooltip() {
        if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
        ItemActionItem item = items.get(hoveredIndex);
        if (item == null) return null;
        return hoveredAction ? item.actionTooltip() : item.tooltip();
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshRange();
        refreshLayout();
        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, 0, 0, zLevel);
        try {
            KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
            try {
                int start = scroll.smoothIndexOffset();
                int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                for (int index = start; index < end; index++) {
                    StateButton rowButton = rowButtons.get(index);
                    StateButton actionButton = actionButtons.get(index);
                    if (!rowButton.visible) continue;
                    int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                            ? mouseX
                            : Integer.MIN_VALUE;
                    rowButton.setInvertedFlash(scroll.isSelectionFlashInverted(index));
                    VanillaGuiDraw.render(rowButton, graphics, clippedMouseX, mouseY, partialTick);
                    renderRowContent(graphics, rowButton, items.get(index), mouseX, mouseY,
                            scroll.isSelectionFlashInverted(index));
                    if (actionButton.visible) VanillaGuiDraw.render(actionButton, graphics, clippedMouseX, mouseY, partialTick);
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
        int actionIndex = actionAt(mouseX, mouseY);
        if (actionIndex >= 0) {
            hoveredIndex = actionIndex;
            hoveredAction = true;
            hoveredStack = ItemStack.EMPTY;
        } else {
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredAction = false;
            hoveredStack = stackAt(mouseX, mouseY);
        }
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    private void renderRowContent(
            GuiGraphics graphics,
            StateButton button,
            ItemActionItem item,
            int mouseX,
            int mouseY,
            boolean invertedFlash
    ) {
        if (item == null) return;
        int slotX = button.getX() + ITEM_LEFT_PADDING;
        int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
        boolean slotHovered = GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
        ItemStack stack = safeStack(item);
        GuiTheme.itemSlot(graphics, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, 2, false, slotHovered, item.error());
        GuiTheme.item(graphics, font, stack, slotX, slotY, ITEM_SLOT_SIZE, 1.0F, false);

        int textX = button.getX() + TEXT_LEFT_PADDING;
        int textWidth = Math.max(1, button.getWidth() - TEXT_LEFT_PADDING - 10);
        KineticText.drawScrollingLeft(
                graphics,
                font,
                itemLabel(item),
                textX,
                button.getY() + Math.max(1, (ROW_HEIGHT - font.lineHeight) / 2),
                textWidth,
                invertedFlash ? GuiTheme.selectionFlashText() : GuiTheme.current().text(),
                true
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
        int actionIndex = actionAt(mouseX, mouseY);
        if (actionIndex >= 0) {
            StateButton actionButton = actionButtons.get(actionIndex);
            return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
        }
        int index = itemAt(mouseX, mouseY);
        return index >= 0 && rowButtons.get(index).active && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
        }
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= items.size()) return;
        ItemActionItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshSelection();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void refreshSelection() {
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton rowButton = rowButtons.get(index);
            StateButton actionButton = actionButtons.get(index);
            ItemActionItem item = items.get(index);
            rowButton.setSelected(index == selectedIndex);
            rowButton.setError(item != null && item.error());
            rowButton.active = active && item != null && item.active();
            actionButton.setSelected(false);
            actionButton.setError(false);
            actionButton.setMessage(item == null || item.actionLabel() == null ? Component.empty() : item.actionLabel());
            actionButton.active = active && item != null && item.actionActive();
        }
        setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                ? itemLabel(items.get(selectedIndex))
                : Component.empty());
    }

    @Override
    protected void refreshLayout() {
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton rowButton = rowButtons.get(index);
            StateButton actionButton = actionButtons.get(index);
            ItemActionItem item = items.get(index);
            boolean rowVisible = visible && index >= start && index < end;
            int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            rowButton.visible = rowVisible;
            rowButton.setX(getX());
            rowButton.setY(rowY);
            rowButton.setWidth(rowWidth());
            actionButton.visible = rowVisible && item != null && item.actionLabel() != null
                    && !item.actionLabel().getString().isBlank();
            actionButton.setX(getX() + rowWidth() + ACTION_GAP);
            actionButton.setY(rowY + Math.max(0, (ROW_HEIGHT - KineticScreen.STANDARD_CONTROL_HEIGHT) / 2));
            actionButton.setWidth(actionWidth);
            rowButton.active = active && rowVisible && item != null && item.active();
            actionButton.active = active && rowVisible && item != null && item.actionActive();
        }
    }

    private int rowIndexAt(double mouseX, double mouseY, boolean action) {
        if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
        int start = scroll.smoothIndexOffset();
        int end = Math.min(items.size(), start + visibleRows() + 1);
        for (int index = start; index < end; index++) {
            StateButton button = action ? actionButtons.get(index) : rowButtons.get(index);
            if (!button.visible || !button.isMouseOver(mouseX, mouseY)) continue;
            return index;
        }
        return -1;
    }

    private int rowWidth() {
        return Math.max(1, contentWidth() - actionWidth - ACTION_GAP);
    }

    private static ItemStack safeStack(ItemActionItem item) {
        return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
    }

    private static Component itemLabel(ItemActionItem item) {
        if (item == null) return Component.empty();
        Component primary = item.label() == null ? Component.empty() : item.label();
        Component secondary = item.secondaryLabel();
        if (secondary == null || secondary.getString().isBlank()) return primary;
        return primary.copy().append("   ").append(secondary);
    }

    @Override
    protected int scrollRangeRows() {
        return items.size();
    }

    @Override
    protected int rowPitch() {
        return ROW_PITCH;
    }
}
