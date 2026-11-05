package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleActionItem;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.ToggleButton;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Smooth scrolling list control behind the public {@code Kinetic*List} API; created only through {@code KineticWidgets}. */
public final class ToggleActionListWidget extends VerticalScrollListWidget implements KineticToggleActionList {
    private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
    private boolean textRows;
    private static final int CONTROL_GAP = 4;

    private final Consumer<Integer> responder;
    private final BiConsumer<Integer, Boolean> toggleResponder;
    private final Consumer<Integer> actionResponder;
    private final int toggleWidth;
    private final int actionWidth;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private final List<ToggleButton> toggleButtons = new ArrayList<>();
    private final List<StateButton> actionButtons = new ArrayList<>();
    private List<ToggleActionItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private int hoveredControl;

    public ToggleActionListWidget(
            FactoryAccess access,
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            int toggleWidth,
            int actionWidth,
            Consumer<Integer> responder,
            BiConsumer<Integer, Boolean> toggleResponder,
            Consumer<Integer> actionResponder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.responder = responder;
        this.toggleResponder = toggleResponder;
        this.actionResponder = actionResponder;
        this.toggleWidth = Math.max(1, toggleWidth);
        this.actionWidth = Math.max(1, actionWidth);
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends ToggleActionItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        toggleButtons.clear();
        actionButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            ToggleActionItem item = items.get(index);
            int rowIndex = index;
            StateButton rowButton = new StateButton(
                    factoryAccess, getX(), getY(), rowWidth(), rowHeight(), itemLabel(item),
                    ignored -> select(rowIndex, true)
            );
            rowButtons.add(rowButton);

            ToggleButton toggleButton = new ToggleButton(
                    factoryAccess, getX(), getY(), this.toggleWidth, rowHeight(),
                    item != null && item.toggleValue(),
                    item == null ? Component.empty() : safe(item.toggleOnLabel()),
                    item == null ? Component.empty() : safe(item.toggleOffLabel()),
                    null,
                    value -> setToggleValueInternal(rowIndex, value, true)
            );
            toggleButtons.add(toggleButton);

            StateButton actionButton = new StateButton(
                    factoryAccess, getX(), getY(), this.actionWidth, rowHeight(),
                    item == null ? Component.empty() : safe(item.actionLabel()),
                    ignored -> {
                        ToggleActionItem current = rowIndex < items.size() ? items.get(rowIndex) : null;
                        if (current == null || !current.actionActive()) return;
                        if (actionResponder != null) actionResponder.accept(rowIndex);
                    }
            );
            actionButtons.add(actionButton);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshState();
        refreshLayout();
    }
    @Override public void setTextRows(boolean textRows) {
        this.textRows = textRows; refreshRange(); refreshState(); refreshLayout();
    }
    private int rowHeight() { return textRows ? 14 : ROW_HEIGHT; }

    @Override
    public List<ToggleActionItem> items() {
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
            refreshState();
            return;
        }
        select(Math.min(items.size() - 1, index), false);
    }

    @Override
    public boolean toggleValue(int index) {
        requireIndex(index);
        ToggleActionItem item = items.get(index);
        return item != null && item.toggleValue();
    }

    @Override
    public void setToggleValue(int index, boolean value) {
        setToggleValueInternal(index, value, false);
    }

    @Override
    public void ensureSelectedVisible() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) return;
        int visible = visibleRows();
        int offset = scroll.offset();
        if (selectedIndex < offset) setScrollOffset(selectedIndex);
        else if (selectedIndex >= offset + visible) setScrollOffset(selectedIndex - visible + 1);
    }

    @Override
    public int itemAt(double mouseX, double mouseY) {
        return rowIndexAt(mouseX, mouseY, 0);
    }

    @Override
    public int toggleAt(double mouseX, double mouseY) {
        return rowIndexAt(mouseX, mouseY, 1);
    }

    @Override
    public int actionAt(double mouseX, double mouseY) {
        return rowIndexAt(mouseX, mouseY, 2);
    }

    @Override
    protected Component hoveredRowTooltip() {
        if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
        ToggleActionItem item = items.get(hoveredIndex);
        if (item == null) return null;
        if (hoveredControl == 1) return item.toggleTooltip();
        if (hoveredControl == 2) return item.actionTooltip();
        return item.tooltip();
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
                int end = rowEnd(start, rowButtons.size());
                for (int index = start; index < end; index++) {
                    StateButton rowButton = rowButtons.get(index);
                    if (!rowButton.visible) continue;
                    int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                            ? mouseX : Integer.MIN_VALUE;
                    rowButton.setInvertedFlash(scroll.isSelectionFlashInverted(index));
                    VanillaGuiDraw.render(rowButton, graphics, clippedMouseX, mouseY, partialTick);
                    var item = items.get(index);
                    if (item.effectIcon() != null) {
                        VanillaGuiDraw.effectIcon(graphics, item.effectIcon(), rowButton.getX() + 3, rowButton.getY() + (rowHeight() - 10) / 2, 10);
                        dev.xyat.kineticcore.internal.client.gui.text.KineticText.drawScrollingLeft(graphics,
                                dev.xyat.kineticcore.api.runtime.KineticClientRuntime.font(), itemLabel(item),
                                rowButton.getX() + 17, rowButton.getY() + (rowHeight() - 8 + 1) / 2, Math.max(1, rowButton.getWidth() - 21),
                                dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme.current().text(), false);
                    }
                    VanillaGuiDraw.render(toggleButtons.get(index), graphics, clippedMouseX, mouseY, partialTick);
                    VanillaGuiDraw.render(actionButtons.get(index), graphics, clippedMouseX, mouseY, partialTick);
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
        int hit = toggleAt(mouseX, mouseY);
        if (hit >= 0) {
            hoveredIndex = hit;
            hoveredControl = 1;
        } else if ((hit = actionAt(mouseX, mouseY)) >= 0) {
            hoveredIndex = hit;
            hoveredControl = 2;
        } else {
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredControl = 0;
        }
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
        // Rows are placed at render time; while the list scrolls smoothly they lag behind the hit test, so a click
        // right after scrolling would miss the row it lands on (or find it inactive). Place them for this frame first.
        refreshRange();
        refreshLayout();
        int index = toggleAt(mouseX, mouseY);
        if (index >= 0) {
            ToggleButton toggleButton = toggleButtons.get(index);
            return toggleButton.active && toggleButton.mouseClicked(mouseX, mouseY, button);
        }
        index = actionAt(mouseX, mouseY);
        if (index >= 0) {
            StateButton actionButton = actionButtons.get(index);
            return actionButton.active && actionButton.mouseClicked(mouseX, mouseY, button);
        }
        index = itemAt(mouseX, mouseY);
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
        ToggleActionItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshState();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void setToggleValueInternal(int index, boolean value, boolean notify) {
        requireIndex(index);
        ToggleActionItem item = items.get(index);
        if (item == null) return;
        List<ToggleActionItem> next = new ArrayList<>(items);
        next.set(index, withToggleValue(item, value));
        items = List.copyOf(next);
        refreshState();
        if (notify && toggleResponder != null) toggleResponder.accept(index, value);
    }

    private void refreshState() {
        for (int index = 0; index < rowButtons.size(); index++) {
            ToggleActionItem item = items.get(index);
            StateButton rowButton = rowButtons.get(index);
            ToggleButton toggleButton = toggleButtons.get(index);
            StateButton actionButton = actionButtons.get(index);
            rowButton.setTextRow(textRows); rowButton.setRowStripe((index & 1) == 1); rowButton.setHeight(rowHeight());
            rowButton.setPicked(textRows && item != null && item.toggleValue());
            rowButton.setSelected(!textRows && index == selectedIndex);
            rowButton.setError(item != null && item.error());
            rowButton.setMessage(itemLabel(item));
            rowButton.setTextVisible(item == null || item.effectIcon() == null);
            rowButton.active = active && item != null && item.active();
            toggleButton.setValue(item != null && item.toggleValue());
            toggleButton.setTextRow(textRows); toggleButton.setHeight(rowHeight()); toggleButton.setPicked(textRows && item != null && item.toggleValue());
            toggleButton.active = active && item != null && item.toggleActive();
            actionButton.setSelected(false);
            actionButton.setHeight(rowHeight());
            actionButton.setError(item != null && item.actionError());
            actionButton.setMessage(item == null ? Component.empty() : safe(item.actionLabel()));
            actionButton.active = active && item != null && item.actionActive();
        }
        setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                ? itemLabel(items.get(selectedIndex)) : Component.empty());
    }

    @Override
    protected void refreshLayout() {
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(rowPitch());
        shiftRows(-scroll.subPixelShift(rowPitch()), rowButtons, toggleButtons, actionButtons);
        int end = rowEnd(start, rowButtons.size());
        for (int index = 0; index < rowButtons.size(); index++) {
            ToggleActionItem item = items.get(index);
            boolean rowVisible = visible && index >= start && index < end;
            int rowY = getY() + (index - start) * rowPitch() - shift + (textRows ? 0 : 2);
            StateButton rowButton = rowButtons.get(index);
            ToggleButton toggleButton = toggleButtons.get(index);
            StateButton actionButton = actionButtons.get(index);
            rowButton.visible = rowVisible;
            rowButton.setX(getX());
            rowButton.setY(rowY);
            rowButton.setWidth(rowWidth());
            rowButton.active = active && rowVisible && item != null && item.active();

            toggleButton.visible = rowVisible;
            toggleButton.setX(getX() + rowWidth() + CONTROL_GAP);
            toggleButton.setY(rowY);
            toggleButton.setWidth(toggleWidth);
            toggleButton.active = active && rowVisible && item != null && item.toggleActive();

            actionButton.visible = rowVisible && item != null && item.actionLabel() != null
                    && !item.actionLabel().getString().isBlank();
            actionButton.setX(getX() + rowWidth() + CONTROL_GAP + toggleWidth + CONTROL_GAP);
            actionButton.setY(rowY);
            actionButton.setWidth(actionWidth);
            actionButton.active = active && rowVisible && item != null && item.actionActive();
        }
    }

    private int rowIndexAt(double mouseX, double mouseY, int control) {
        if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(rowPitch());
        int end = rowEnd(start, items.size());
        for (int index = start; index < end; index++) {
            AbstractWidget widget = control == 1 ? toggleButtons.get(index)
                    : control == 2 ? actionButtons.get(index) : rowButtons.get(index);
            if (!widget.visible) continue;
            int rowY = getY() + (index - start) * rowPitch() - shift + (textRows ? 0 : 2);
            if (mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= rowY && mouseY < rowY + rowHeight()) return index;
        }
        return -1;
    }

    private int rowWidth() {
        return Math.max(1, contentWidth() - toggleWidth - actionWidth - CONTROL_GAP * 2);
    }

    private void requireIndex(int index) {
        if (index < 0 || index >= items.size()) throw new IndexOutOfBoundsException(index);
    }

    private static ToggleActionItem withToggleValue(ToggleActionItem item, boolean value) {
        return new ToggleActionItem(
                item.label(), item.secondaryLabel(), item.tooltip(), item.active(), item.error(), value,
                item.toggleOnLabel(), item.toggleOffLabel(), item.toggleTooltip(), item.toggleActive(),
                item.actionLabel(), item.actionTooltip(), item.actionActive(), item.actionError(), item.effectIcon()
        );
    }

    private static Component itemLabel(ToggleActionItem item) {
        if (item == null) return Component.empty();
        Component primary = safe(item.label());
        Component secondary = item.secondaryLabel();
        if (secondary == null || secondary.getString().isBlank()) return primary;
        return primary.copy().append("   ").append(secondary);
    }

    private static Component safe(Component component) {
        return component == null ? Component.empty() : component;
    }

    @Override
    protected int scrollRangeRows() {
        return items.size();
    }

    @Override
    protected int rowPitch() {
        return rowHeight() + (textRows ? 2 : 5);
    }
}
