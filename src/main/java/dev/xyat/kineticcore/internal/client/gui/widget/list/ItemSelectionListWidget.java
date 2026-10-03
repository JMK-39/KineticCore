package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemSelectionList;
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
public final class ItemSelectionListWidget extends VerticalScrollListWidget implements KineticItemSelectionList {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_PITCH = ROW_HEIGHT + 2;
    private static final int ROW_TOP_PADDING = 1;
    private static final int ITEM_SLOT_SIZE = 18;
    private static final int ITEM_LEFT_PADDING = 2;
    private static final int TEXT_LEFT_PADDING = 24;

    private final Font font;
    private final Consumer<Integer> responder;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private List<ItemSelectionItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public ItemSelectionListWidget(
            FactoryAccess access,
            Font font,
            int x,
            int y,
            int width,
            int height,
            List<? extends ItemSelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.font = font;
        this.responder = responder;
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends ItemSelectionItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            ItemSelectionItem item = items.get(index);
            int rowIndex = index;
            StateButton button = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    contentWidth(),
                    ROW_HEIGHT,
                    Component.empty(),
                    ignored -> select(rowIndex, true)
            );
            button.active = item != null && item.active();
            button.setError(item != null && item.error());
            rowButtons.add(button);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshSelection();
        refreshLayout();
    }

    @Override
    public List<ItemSelectionItem> items() {
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
        if (!visible || mouseX < getX() || mouseX >= getX() + contentWidth()
                || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        for (int index = start; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            if (rowY >= getY() + getHeight()) break;
            if (rowY + ROW_HEIGHT <= getY()) continue;
            if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
        }
        return -1;
    }

    @Override
    public ItemStack stackAt(double mouseX, double mouseY) {
        int index = itemAt(mouseX, mouseY);
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        StateButton button = rowButtons.get(index);
        int slotX = button.getX() + ITEM_LEFT_PADDING;
        int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
        if (!GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE)) return ItemStack.EMPTY;
        ItemSelectionItem item = items.get(index);
        return safeStack(item);
    }

    @Override
    public ItemStack hoveredStack() {
        return hoveredStack;
    }

    @Override
    protected Component hoveredRowTooltip() {
        return hoveredIndex >= 0 && hoveredIndex < items.size() && items.get(hoveredIndex) != null
                ? items.get(hoveredIndex).tooltip()
                : null;
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
                    StateButton button = rowButtons.get(index);
                    if (!button.visible) continue;
                    int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                            ? mouseX
                            : Integer.MIN_VALUE;
                    button.setInvertedFlash(scroll.isSelectionFlashInverted(index));
                    VanillaGuiDraw.render(button, graphics, clippedMouseX, mouseY, partialTick);
                    renderRowContent(graphics, button, items.get(index), mouseX, mouseY,
                            scroll.isSelectionFlashInverted(index));
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
        hoveredIndex = itemAt(mouseX, mouseY);
        hoveredStack = stackAt(mouseX, mouseY);
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    private void renderRowContent(
            GuiGraphics graphics,
            StateButton button,
            ItemSelectionItem item,
            int mouseX,
            int mouseY,
            boolean invertedFlash
    ) {
        if (item == null) return;
        int slotX = button.getX() + ITEM_LEFT_PADDING;
        int slotY = button.getY() + Math.max(0, (ROW_HEIGHT - ITEM_SLOT_SIZE) / 2);
        boolean slotHovered = GuiTheme.hovering(mouseX, mouseY, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
        ItemStack stack = safeStack(item);
        GuiTheme.itemSlot(graphics, slotX, slotY, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE, 4, false, slotHovered, item.error());
        GuiTheme.item(graphics, font, stack, slotX, slotY, ITEM_SLOT_SIZE, 1.0F, false);

        int textX = button.getX() + TEXT_LEFT_PADDING;
        int textWidth = Math.max(1, button.getWidth() - TEXT_LEFT_PADDING - 10);
        KineticText.drawScrollingLeft(
                graphics, font, itemLabel(item), textX,
                button.getY() + Math.max(1, (ROW_HEIGHT - font.lineHeight) / 2),
                textWidth, invertedFlash ? GuiTheme.selectionFlashText() : GuiTheme.current().text(), true
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
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
        ItemSelectionItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshSelection();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void refreshSelection() {
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            ItemSelectionItem item = items.get(index);
            button.setSelected(index == selectedIndex);
            button.setError(item != null && item.error());
            button.active = active && item != null && item.active();
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
            StateButton button = rowButtons.get(index);
            boolean rowVisible = visible && index >= start && index < end;
            button.visible = rowVisible;
            button.setX(getX());
            button.setY(getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING);
            button.setWidth(contentWidth());
            ItemSelectionItem item = items.get(index);
            button.active = active && rowVisible && item != null && item.active();
        }
    }

    private static ItemStack safeStack(ItemSelectionItem item) {
        return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
    }

    private static Component itemLabel(ItemSelectionItem item) {
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
