package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticMultiToggleList;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiToggleItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.RowToggle;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleHit;
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
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Smooth scrolling list control behind the public {@code Kinetic*List} API; created only through {@code KineticWidgets}. */
public final class MultiToggleListWidget extends VerticalScrollListWidget implements KineticMultiToggleList {
    private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
    private static final int ROW_PITCH = ROW_HEIGHT + 5;
    private static final int ROW_TOP_PADDING = 2;
    private static final int TOGGLE_GAP = 2;

    private final Consumer<Integer> responder;
    private final BiConsumer<ToggleHit, Boolean> toggleResponder;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private final List<List<ToggleButton>> toggleButtons = new ArrayList<>();
    private List<MultiToggleItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private int hoveredToggleIndex = -1;

    public MultiToggleListWidget(
            FactoryAccess access,
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiToggleItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<ToggleHit, Boolean> toggleResponder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.responder = responder;
        this.toggleResponder = toggleResponder;
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends MultiToggleItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        toggleButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            MultiToggleItem item = items.get(index);
            int rowIndex = index;
            StateButton rowButton = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    rowWidth(item),
                    ROW_HEIGHT,
                    itemLabel(item),
                    ignored -> select(rowIndex, true)
            );
            rowButtons.add(rowButton);

            List<RowToggle> toggles = toggles(item);
            List<ToggleButton> buttons = new ArrayList<>(toggles.size());
            for (int toggleIndex = 0; toggleIndex < toggles.size(); toggleIndex++) {
                RowToggle toggle = toggles.get(toggleIndex);
                int currentToggleIndex = toggleIndex;
                ToggleButton button = new ToggleButton(
                        factoryAccess,
                        getX(),
                        getY(),
                        Math.max(1, toggle.width()),
                        ROW_HEIGHT,
                        toggle.value(),
                        safe(toggle.onLabel()),
                        safe(toggle.offLabel()),
                        null,
                        value -> setToggleValueInternal(rowIndex, currentToggleIndex, value, true)
                );
                button.setError(toggle.error());
                buttons.add(button);
            }
            toggleButtons.add(buttons);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshState();
        refreshLayout();
    }

    @Override
    public List<MultiToggleItem> items() {
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
    public boolean toggleValue(int rowIndex, int toggleIndex) {
        RowToggle toggle = requireToggle(rowIndex, toggleIndex);
        return toggle.value();
    }

    @Override
    public void setToggleValue(int rowIndex, int toggleIndex, boolean value) {
        setToggleValueInternal(rowIndex, toggleIndex, value, false);
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
        if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return -1;
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        int end = Math.min(items.size(), start + visibleRows() + 1);
        for (int index = start; index < end; index++) {
            StateButton button = rowButtons.get(index);
            if (!button.visible) continue;
            int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) return index;
        }
        return -1;
    }

    @Override
    public ToggleHit toggleAt(double mouseX, double mouseY) {
        if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return null;
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        int end = Math.min(items.size(), start + visibleRows() + 1);
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            List<ToggleButton> buttons = toggleButtons.get(rowIndex);
            for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                ToggleButton button = buttons.get(toggleIndex);
                if (!button.visible) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    return new ToggleHit(rowIndex, toggleIndex);
                }
            }
        }
        return null;
    }

    @Override
    protected Component hoveredRowTooltip() {
        if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
        MultiToggleItem item = items.get(hoveredIndex);
        if (item == null) return null;
        if (hoveredToggleIndex >= 0) {
            List<RowToggle> toggles = toggles(item);
            if (hoveredToggleIndex < toggles.size()) return toggles.get(hoveredToggleIndex).tooltip();
        }
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
                int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                for (int index = start; index < end; index++) {
                    StateButton rowButton = rowButtons.get(index);
                    if (!rowButton.visible) continue;
                    int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                            ? mouseX : Integer.MIN_VALUE;
                    rowButton.setInvertedFlash(scroll.isSelectionFlashInverted(index));
                    VanillaGuiDraw.render(rowButton, graphics, clippedMouseX, mouseY, partialTick);
                    for (ToggleButton toggleButton : toggleButtons.get(index)) {
                        if (toggleButton.visible) VanillaGuiDraw.render(toggleButton, graphics, clippedMouseX, mouseY, partialTick);
                    }
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
        ToggleHit hit = toggleAt(mouseX, mouseY);
        if (hit != null) {
            hoveredIndex = hit.rowIndex();
            hoveredToggleIndex = hit.toggleIndex();
        } else {
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredToggleIndex = -1;
        }
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
        ToggleHit hit = toggleAt(mouseX, mouseY);
        if (hit != null) {
            ToggleButton toggleButton = toggleButtons.get(hit.rowIndex()).get(hit.toggleIndex());
            return toggleButton.active && toggleButton.mouseClicked(mouseX, mouseY, button);
        }
        int index = itemAt(mouseX, mouseY);
        return index >= 0 && rowButtons.get(index).active
                && rowButtons.get(index).mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            output.add(NarratedElementType.TITLE, itemLabel(items.get(selectedIndex)));
        }
    }

    private void select(int index, boolean notify) {
        if (index < 0 || index >= items.size()) return;
        MultiToggleItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshState();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void setToggleValueInternal(int rowIndex, int toggleIndex, boolean value, boolean notify) {
        RowToggle toggle = requireToggle(rowIndex, toggleIndex);
        MultiToggleItem item = items.get(rowIndex);
        List<RowToggle> nextToggles = new ArrayList<>(toggles(item));
        nextToggles.set(toggleIndex, withValue(toggle, value));
        List<MultiToggleItem> nextItems = new ArrayList<>(items);
        nextItems.set(rowIndex, new MultiToggleItem(
                item.label(), item.secondaryLabel(), item.tooltip(), item.active(), item.error(),
                List.copyOf(nextToggles)
        ));
        items = List.copyOf(nextItems);
        refreshState();
        if (notify && toggleResponder != null) toggleResponder.accept(new ToggleHit(rowIndex, toggleIndex), value);
    }

    private void refreshState() {
        for (int rowIndex = 0; rowIndex < rowButtons.size(); rowIndex++) {
            MultiToggleItem item = items.get(rowIndex);
            StateButton rowButton = rowButtons.get(rowIndex);
            rowButton.setSelected(rowIndex == selectedIndex);
            rowButton.setError(item != null && item.error());
            rowButton.setMessage(itemLabel(item));
            rowButton.active = active && item != null && item.active();

            List<RowToggle> toggles = toggles(item);
            List<ToggleButton> buttons = toggleButtons.get(rowIndex);
            for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                RowToggle toggle = toggles.get(toggleIndex);
                ToggleButton button = buttons.get(toggleIndex);
                button.setValue(toggle.value());
                button.setError(toggle.error());
                button.active = active && toggle.active();
            }
        }
        setMessage(selectedIndex >= 0 && selectedIndex < items.size()
                ? itemLabel(items.get(selectedIndex)) : Component.empty());
    }

    @Override
    protected void refreshLayout() {
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
        for (int rowIndex = 0; rowIndex < rowButtons.size(); rowIndex++) {
            MultiToggleItem item = items.get(rowIndex);
            boolean rowVisible = visible && rowIndex >= start && rowIndex < end;
            int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            StateButton rowButton = rowButtons.get(rowIndex);
            rowButton.visible = rowVisible;
            rowButton.setX(getX());
            rowButton.setY(rowY);
            rowButton.setWidth(rowWidth(item));
            rowButton.active = active && rowVisible && item != null && item.active();

            int toggleX = getX() + rowButton.getWidth() + TOGGLE_GAP;
            List<RowToggle> toggles = toggles(item);
            List<ToggleButton> buttons = toggleButtons.get(rowIndex);
            for (int toggleIndex = 0; toggleIndex < buttons.size(); toggleIndex++) {
                RowToggle toggle = toggles.get(toggleIndex);
                ToggleButton button = buttons.get(toggleIndex);
                button.visible = rowVisible;
                button.setX(toggleX);
                button.setY(rowY);
                button.setWidth(Math.max(1, toggle.width()));
                button.active = active && rowVisible && toggle.active();
                toggleX += Math.max(1, toggle.width()) + TOGGLE_GAP;
            }
        }
    }

    private int rowWidth(MultiToggleItem item) {
        int toggleSpace = 0;
        for (RowToggle toggle : toggles(item)) {
            toggleSpace += Math.max(1, toggle.width()) + TOGGLE_GAP;
        }
        return Math.max(1, contentWidth() - toggleSpace);
    }

    private RowToggle requireToggle(int rowIndex, int toggleIndex) {
        if (rowIndex < 0 || rowIndex >= items.size()) throw new IndexOutOfBoundsException(rowIndex);
        List<RowToggle> toggles = toggles(items.get(rowIndex));
        if (toggleIndex < 0 || toggleIndex >= toggles.size()) throw new IndexOutOfBoundsException(toggleIndex);
        return toggles.get(toggleIndex);
    }

    private static List<RowToggle> toggles(MultiToggleItem item) {
        return item == null || item.toggles() == null ? List.of() : item.toggles();
    }

    private static RowToggle withValue(RowToggle toggle, boolean value) {
        return new RowToggle(
                toggle.onLabel(), toggle.offLabel(), toggle.tooltip(), toggle.width(), value,
                toggle.active(), toggle.error()
        );
    }

    private static Component itemLabel(MultiToggleItem item) {
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
        return ROW_PITCH;
    }
}
