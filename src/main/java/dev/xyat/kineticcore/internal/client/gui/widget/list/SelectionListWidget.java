package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticSelectionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Smooth scrolling list control behind the public {@code Kinetic*List} API; created only through {@code KineticWidgets}. */
public final class SelectionListWidget extends VerticalScrollListWidget implements KineticSelectionList {
    private static final int BUTTON_ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
    // Plain text rows: 8 px glyphs with 3 px above and below, packed without gaps.
    private static final int TEXT_ROW_HEIGHT = 14;
    private boolean textRows;
    private int rowHeight = BUTTON_ROW_HEIGHT;
    private int rowStep = BUTTON_ROW_HEIGHT + 5;
    private int rowTopPadding = 2;

    private final Consumer<Integer> responder;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private List<SelectionItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;

    public SelectionListWidget(
            FactoryAccess access,
            int x,
            int y,
            int width,
            int height,
            List<? extends SelectionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.responder = responder;
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends SelectionItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            SelectionItem item = items.get(index);
            int rowIndex = index;
            StateButton button = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    contentWidth(),
                    rowHeight,
                    itemLabel(item),
                    ignored -> select(rowIndex, true)
            );
            button.active = item != null && item.active();
            button.setError(item != null && item.error());
            button.setTextRow(textRows);
            button.setRowStripe((index & 1) == 1);
            rowButtons.add(button);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshSelection();
        refreshLayout();
    }

    @Override
    public List<SelectionItem> items() {
        return List.copyOf(items);
    }

    @Override
    public int selectedIndex() {
        return selectedIndex;
    }

    @Override
    public void setSelectedIndex(int index) {
        if (items.isEmpty()) {
            selectedIndex = -1;
            refreshSelection();
            return;
        }
        if (index < 0) {
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
        int shift = scroll.visualShift(rowStep);
        for (int index = start; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            int rowY = getY() + (index - start) * rowStep - shift + rowTopPadding;
            if (rowY >= getY() + getHeight()) break;
            if (rowY + rowHeight <= getY()) continue;
            if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                    && mouseY >= rowY && mouseY < rowY + rowHeight) return index;
        }
        return -1;
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
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
        hoveredIndex = itemAt(mouseX, mouseY);
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
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
        SelectionItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshSelection();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void refreshSelection() {
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            SelectionItem item = items.get(index);
            button.setSelected(index == selectedIndex);
            button.setError(item != null && item.error());
            button.setMessage(itemLabel(item));
            button.active = active && item != null && item.active();
        }
        setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                ? itemLabel(items.get(selectedIndex))
                : Component.empty());
    }

    @Override
    protected void refreshLayout() {
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(rowStep);
        int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            boolean rowVisible = visible && index >= start && index < end;
            button.visible = rowVisible;
            button.setX(getX());
            button.setY(getY() + (index - start) * rowStep - shift + rowTopPadding);
            button.setWidth(contentWidth());
            SelectionItem item = items.get(index);
            button.active = active && rowVisible && item != null && item.active();
        }
    }

    private static Component itemLabel(SelectionItem item) {
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
    public void setTextRows(boolean textRows) {
        this.textRows = textRows;
        rowHeight = textRows ? TEXT_ROW_HEIGHT : BUTTON_ROW_HEIGHT;
        rowStep = textRows ? TEXT_ROW_HEIGHT : BUTTON_ROW_HEIGHT + 5;
        rowTopPadding = textRows ? 0 : 2;
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            button.setTextRow(textRows);
            button.setRowStripe((index & 1) == 1);
            button.setHeight(rowHeight);
        }
        refreshRange();
        refreshLayout();
    }

    @Override
    public boolean textRows() {
        return textRows;
    }

    @Override
    protected int rowPitch() {
        return rowStep;
    }
}
