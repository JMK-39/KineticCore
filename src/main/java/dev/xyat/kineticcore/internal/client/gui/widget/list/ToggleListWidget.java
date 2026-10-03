package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleList;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleItem;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Smooth scrolling list control behind the public {@code Kinetic*List} API; created only through {@code KineticWidgets}. */
public final class ToggleListWidget extends VerticalScrollListWidget implements KineticToggleList {
    private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
    private static final int ROW_PITCH = ROW_HEIGHT + 5;
    private static final int ROW_TOP_PADDING = 2;

    private final BiConsumer<Integer, Boolean> responder;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private List<ToggleItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;

    public ToggleListWidget(
            FactoryAccess access,
            int x,
            int y,
            int width,
            int height,
            List<? extends ToggleItem> items,
            int initialScrollOffset,
            BiConsumer<Integer, Boolean> responder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.responder = responder == null ? (index, value) -> { } : responder;
        this.zLevel = zLevel;
        setItems(items);
    }

    @Override
    public void setItems(List<? extends ToggleItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        if (selectedIndex < 0 || selectedIndex >= items.size()
                || !items.get(selectedIndex).value()) {
            int modelSelectedIndex = findSelectedItem();
            if (modelSelectedIndex >= 0) selectedIndex = modelSelectedIndex;
            else if (selectedIndex >= items.size()) selectedIndex = -1;
        }
        rowButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            ToggleItem item = items.get(index);
            int rowIndex = index;
            StateButton button = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    contentWidth(),
                    ROW_HEIGHT,
                    itemLabel(item),
                    ignored -> toggle(rowIndex)
            );
            rowButtons.add(button);
        }
        refreshRange();
        refreshValues();
        refreshLayout();
    }

    @Override
    public List<ToggleItem> items() {
        return List.copyOf(items);
    }

    @Override
    public boolean value(int index) {
        requireIndex(index);
        ToggleItem item = items.get(index);
        return item != null && item.value();
    }

    @Override
    public void setValue(int index, boolean value) {
        setValueInternal(index, value, false);
    }

    @Override
    public void setValues(List<Boolean> values) {
        if (values == null || values.size() != items.size()) {
            throw new IllegalArgumentException("values size must match items size");
        }
        List<ToggleItem> next = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            ToggleItem item = items.get(index);
            next.add(withValue(item, Boolean.TRUE.equals(values.get(index))));
        }
        items = List.copyOf(next);
        refreshValues();
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
        if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
            output.add(NarratedElementType.TITLE, itemLabel(items.get(hoveredIndex)));
        }
    }

    private void toggle(int index) {
        requireIndex(index);
        ToggleItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        setValueInternal(index, !item.value(), true);
    }

    private int findSelectedItem() {
        for (int index = items.size() - 1; index >= 0; index--) {
            ToggleItem item = items.get(index);
            if (item != null && item.value()) return index;
        }
        return -1;
    }

    private void setValueInternal(int index, boolean value, boolean notify) {
        requireIndex(index);
        ToggleItem item = items.get(index);
        if (item == null) return;
        List<ToggleItem> next = new ArrayList<>(items);
        next.set(index, withValue(item, value));
        items = List.copyOf(next);
        refreshValues();
        if (notify) responder.accept(index, value);
    }

    private void refreshValues() {
        for (int index = 0; index < rowButtons.size(); index++) {
            StateButton button = rowButtons.get(index);
            ToggleItem item = items.get(index);
            button.setSelected(item != null && item.value());
            button.setMessage(itemLabel(item));
            button.active = active && item != null && item.active();
        }
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
            ToggleItem item = items.get(index);
            button.active = active && rowVisible && item != null && item.active();
        }
    }

    private void requireIndex(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IllegalArgumentException("index out of range: " + index + " for " + items.size() + " items");
        }
    }

    private static ToggleItem withValue(ToggleItem item, boolean value) {
        return item == null ? null : new ToggleItem(item.label(), item.tooltip(), value, item.active());
    }

    private static Component itemLabel(ToggleItem item) {
        return item == null || item.label() == null ? Component.empty() : item.label();
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
