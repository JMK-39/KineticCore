package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.widget.list.ActionHit;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticMultiActionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.MultiActionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.RowAction;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.gui.widget.button.KineticButtons.StateButton;
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
public final class MultiActionListWidget extends VerticalScrollListWidget implements KineticMultiActionList {
    private static final int ROW_HEIGHT = KineticScreen.STANDARD_CONTROL_HEIGHT;
    private static final int ROW_PITCH = ROW_HEIGHT + 5;
    private static final int ROW_TOP_PADDING = 2;
    private static final int ACTION_GAP = 2;

    private final Consumer<Integer> responder;
    private final BiConsumer<Integer, Integer> actionResponder;
    private final int zLevel;
    private final List<StateButton> rowButtons = new ArrayList<>();
    private final List<List<StateButton>> actionButtons = new ArrayList<>();
    private List<MultiActionItem> items = List.of();
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private int hoveredActionIndex = -1;

    public MultiActionListWidget(
            FactoryAccess access,
            int x,
            int y,
            int width,
            int height,
            List<? extends MultiActionItem> items,
            int selectedIndex,
            int initialScrollOffset,
            Consumer<Integer> responder,
            BiConsumer<Integer, Integer> actionResponder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.responder = responder;
        this.actionResponder = actionResponder;
        this.zLevel = zLevel;
        setItems(items);
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void setItems(List<? extends MultiActionItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        rowButtons.clear();
        actionButtons.clear();
        for (int rowIndex = 0; rowIndex < items.size(); rowIndex++) {
            MultiActionItem item = items.get(rowIndex);
            int currentRow = rowIndex;
            StateButton rowButton = new StateButton(
                    factoryAccess,
                    getX(),
                    getY(),
                    rowWidth(item),
                    ROW_HEIGHT,
                    itemLabel(item),
                    ignored -> select(currentRow, true)
            );
            rowButton.active = item != null && item.active();
            rowButton.setError(item != null && item.error());
            rowButtons.add(rowButton);

            List<StateButton> rowActions = new ArrayList<>();
            List<RowAction> actions = actions(item);
            for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
                RowAction action = actions.get(actionIndex);
                int currentAction = actionIndex;
                StateButton actionButton = new StateButton(
                        factoryAccess,
                        getX(),
                        getY(),
                        Math.max(1, action.width()),
                        ROW_HEIGHT,
                        action.label() == null ? Component.empty() : action.label(),
                        ignored -> {
                            if (!action.active()) return;
                            if (actionResponder != null) actionResponder.accept(currentRow, currentAction);
                        }
                );
                actionButton.active = action.active();
                actionButton.setError(action.error());
                rowActions.add(actionButton);
            }
            actionButtons.add(rowActions);
        }
        if (items.isEmpty()) selectedIndex = -1;
        else if (selectedIndex >= items.size()) selectedIndex = items.size() - 1;
        refreshRange();
        refreshSelection();
        refreshLayout();
    }

    @Override
    public List<MultiActionItem> items() {
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
    public ActionHit actionAt(double mouseX, double mouseY) {
        if (!visible || mouseY < getY() || mouseY >= getY() + getHeight()) return null;
        int start = scroll.smoothIndexOffset();
        int shift = scroll.visualShift(ROW_PITCH);
        int end = Math.min(items.size(), start + visibleRows() + 1);
        for (int rowIndex = start; rowIndex < end; rowIndex++) {
            int rowY = getY() + (rowIndex - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            List<StateButton> buttons = actionButtons.get(rowIndex);
            for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                StateButton button = buttons.get(actionIndex);
                if (!button.visible) continue;
                if (mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    return new ActionHit(rowIndex, actionIndex);
                }
            }
        }
        return null;
    }

    @Override
    protected Component hoveredRowTooltip() {
        if (hoveredIndex < 0 || hoveredIndex >= items.size()) return null;
        MultiActionItem item = items.get(hoveredIndex);
        if (item == null) return null;
        if (hoveredActionIndex >= 0) {
            List<RowAction> actions = actions(item);
            if (hoveredActionIndex >= actions.size()) return null;
            return actions.get(hoveredActionIndex).tooltip();
        }
        return item.tooltip();
    }

    @Override
    protected void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshRange();
        refreshLayout();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, zLevel);
        try {
            KineticRenderRuntime.enableScissor(graphics, getX(), getY(), getX() + contentWidth(), getY() + getHeight());
            try {
                int start = scroll.smoothIndexOffset();
                int end = Math.min(rowButtons.size(), start + visibleRows() + 1);
                for (int index = start; index < end; index++) {
                    StateButton rowButton = rowButtons.get(index);
                    if (!rowButton.visible) continue;
                    int clippedMouseX = mouseX >= getX() && mouseX < getX() + contentWidth()
                            ? mouseX
                            : Integer.MIN_VALUE;
                    rowButton.setInvertedFlash(scroll.isSelectionFlashInverted(index));
                    rowButton.render(graphics, clippedMouseX, mouseY, partialTick);
                    for (StateButton actionButton : actionButtons.get(index)) {
                        if (actionButton.visible) actionButton.render(graphics, clippedMouseX, mouseY, partialTick);
                    }
                }
            } finally {
                KineticRenderRuntime.disableScissor(graphics);
            }
            renderScrollbar(graphics, mouseX, mouseY);
        } finally {
            graphics.pose().popPose();
        }
        ActionHit hit = actionAt(mouseX, mouseY);
        if (hit != null) {
            hoveredIndex = hit.rowIndex();
            hoveredActionIndex = hit.actionIndex();
        } else {
            hoveredIndex = itemAt(mouseX, mouseY);
            hoveredActionIndex = -1;
        }
        KineticControlBridge.setHoverTooltip(this, hoveredTooltip());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
        ActionHit hit = actionAt(mouseX, mouseY);
        if (hit != null) {
            StateButton actionButton = actionButtons.get(hit.rowIndex()).get(hit.actionIndex());
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
        MultiActionItem item = items.get(index);
        if (item == null || !item.active()) return;
        selectedIndex = index;
        refreshSelection();
        ensureSelectedVisible();
        if (notify && responder != null) responder.accept(index);
    }

    private void refreshSelection() {
        for (int index = 0; index < rowButtons.size(); index++) {
            MultiActionItem item = items.get(index);
            StateButton rowButton = rowButtons.get(index);
            rowButton.setSelected(index == selectedIndex);
            rowButton.setError(item != null && item.error());
            rowButton.setMessage(itemLabel(item));
            rowButton.active = active && item != null && item.active();

            List<RowAction> actions = actions(item);
            List<StateButton> buttons = actionButtons.get(index);
            for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                RowAction action = actions.get(actionIndex);
                StateButton button = buttons.get(actionIndex);
                button.setSelected(false);
                button.setError(action.error());
                button.setMessage(action.label() == null ? Component.empty() : action.label());
                button.active = active && action.active();
            }
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
            MultiActionItem item = items.get(index);
            StateButton rowButton = rowButtons.get(index);
            boolean rowVisible = visible && index >= start && index < end;
            int rowY = getY() + (index - start) * ROW_PITCH - shift + ROW_TOP_PADDING;
            rowButton.visible = rowVisible;
            rowButton.setX(getX());
            rowButton.setY(rowY);
            rowButton.setWidth(rowWidth(item));
            rowButton.active = active && rowVisible && item != null && item.active();

            int actionX = getX() + rowButton.getWidth() + ACTION_GAP;
            List<RowAction> actions = actions(item);
            List<StateButton> buttons = actionButtons.get(index);
            for (int actionIndex = 0; actionIndex < buttons.size(); actionIndex++) {
                RowAction action = actions.get(actionIndex);
                StateButton button = buttons.get(actionIndex);
                boolean hasLabel = action.label() != null && !action.label().getString().isBlank();
                button.visible = rowVisible && hasLabel;
                button.setX(actionX);
                button.setY(rowY);
                button.setWidth(Math.max(1, action.width()));
                button.active = active && rowVisible && action.active();
                if (hasLabel) actionX += Math.max(1, action.width()) + ACTION_GAP;
            }
        }
    }

    private int rowWidth(MultiActionItem item) {
        int actionSpace = 0;
        List<RowAction> actions = actions(item);
        for (RowAction action : actions) {
            if (action.label() == null || action.label().getString().isBlank()) continue;
            actionSpace += Math.max(1, action.width());
            actionSpace += ACTION_GAP;
        }
        return Math.max(1, contentWidth() - actionSpace);
    }

    private static List<RowAction> actions(MultiActionItem item) {
        return item == null || item.actions() == null ? List.of() : item.actions();
    }

    private static Component itemLabel(MultiActionItem item) {
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
