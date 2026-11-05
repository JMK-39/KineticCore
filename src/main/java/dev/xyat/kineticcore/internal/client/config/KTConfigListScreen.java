package dev.xyat.kineticcore.internal.client.config;

import dev.xyat.kineticcore.api.config.client.KTConfigEntry;
import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;


import dev.xyat.kineticcore.internal.client.gui.text.KineticText;
import dev.xyat.kineticcore.api.runtime.KineticClientRuntime;
import dev.xyat.kineticcore.internal.client.gui.screen.KineticScreen;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.GridScrollController;
import dev.xyat.kineticcore.internal.client.gui.widget.input.KineticTextFields.KineticEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class KTConfigListScreen extends KineticScreen {
    private static final int LIST_X = 4;
    // Header row (Back, title, Add, Done), one description line, then the rows down to the bottom margin.
    private static final int LIST_Y = 44;
    // Item rows are taller so the item sits in a standard 22 px slot.
    private final int rowHeight;
    private final int visibleRows;
    private final int listHeight;
    private static final int SCROLL_X = 632;
    private static final int SCROLL_WIDTH = 4;
    private static final int EDIT_X = 34;
    private static final int EDIT_WIDTH = 476;
    private static final int ROW_RIGHT = 628;
    private static final int NUMBER_X = 8;
    private static final int UP_X = 516, DOWN_X = 554, REMOVE_X = 592;
    // Item rows: the number field sits left of the move buttons.
    private static final int VALUE_W = 96, VALUE_X = UP_X - 6 - VALUE_W;
    private static final long DRAG_SCROLL_INTERVAL_NANOS = 120_000_000L;

    private final Component description;
    private final boolean integerList;
    private final KTConfigEntry.ListStyle style;
    private final boolean fixedItems;
    private final boolean itemValues;
    private final Consumer<List<?>> resultConsumer;
    private final List<String> values = new ArrayList<>();
    private final List<String> originalValues = new ArrayList<>();
    private final GridScrollController listScroll = new GridScrollController();
    private final List<AbstractWidget> listWidgets = new ArrayList<>();
    private Component status;
    private int draggingIndex = -1;
    private int dragTargetIndex = -1;
    private double dragMouseX;
    private double dragMouseY;
    private double dragGrabOffsetX;
    private double dragGrabOffsetY;
    private long lastDragScrollNanos;

    KTConfigListScreen(
            Component title,
            Component description,
            boolean integerList,
            List<?> initialValues,
            Consumer<List<?>> resultConsumer
    ) {
        this(title, description, integerList, KTConfigEntry.ListStyle.TEXT, initialValues, resultConsumer);
    }

    KTConfigListScreen(
            Component title,
            Component description,
            boolean integerList,
            KTConfigEntry.ListStyle style,
            List<?> initialValues,
            Consumer<List<?>> resultConsumer
    ) {
        super(title);
        this.description = description;
        this.integerList = integerList;
        this.style = style == null ? KTConfigEntry.ListStyle.TEXT : style;
        this.fixedItems = this.style == KTConfigEntry.ListStyle.FIXED;
        this.itemValues = this.style == KTConfigEntry.ListStyle.ITEM_VALUE;
        this.rowHeight = itemValues ? 24 : 20;
        this.visibleRows = (356 - LIST_Y) / rowHeight;
        this.listHeight = visibleRows * rowHeight;
        this.resultConsumer = resultConsumer;
        for (Object value : initialValues) values.add(String.valueOf(value));
        originalValues.addAll(values);
    }

    @Override
    protected void buildUi() {
        resetScrollableWidgets();
        listWidgets.clear();
        listScroll.update(values.size(), visibleRows);
        for (int index = 0; index < values.size(); index++) {
            int capturedIndex = index;
            int y = LIST_Y + index * rowHeight;
            KineticEditBox box;
            if (itemValues) {
                box = addIntegerField(VALUE_X, y + (rowHeight - STANDARD_CONTROL_HEIGHT) / 2, VALUE_W, title,
                        false, 1, Integer.MAX_VALUE, null, null);
                box.setValue(itemValueNumber(values.get(index)));
                box.setResponder(value -> values.set(capturedIndex, itemValueId(values.get(capturedIndex)) + "|" + value.trim()));
                addListScrollableWidget(box);
                box = null;
            } else if (fixedItems) {
                box = null;
            } else if (integerList) {
                box = addIntegerField(
                        EDIT_X, y + 2, EDIT_WIDTH, title,
                        true, Integer.MIN_VALUE, Integer.MAX_VALUE, null, null
                );
            } else {
                box = addTextField(EDIT_X, y + 2, EDIT_WIDTH, title, null, null, null);
                box.setMaxLength(32767);
            }
            if (box != null) {
                box.setValue(values.get(index));
                box.setResponder(value -> values.set(capturedIndex, value));
                addListScrollableWidget(box);
            }

            int buttonY = y + (rowHeight - STANDARD_CONTROL_HEIGHT) / 2;
            addListScrollableWidget(addButton(
                    UP_X, buttonY, 32,
                    KineticText.translatable("gui.kineticcore.config.move_up"),
                    null,
                    () -> move(capturedIndex, -1)
            ));
            addListScrollableWidget(addButton(
                    DOWN_X, buttonY, 32,
                    KineticText.translatable("gui.kineticcore.config.move_down"),
                    null,
                    () -> move(capturedIndex, 1)
            ));
            addListScrollableWidget(addButton(
                    REMOVE_X, buttonY, 34,
                    KineticText.translatable("gui.kineticcore.config.remove_short"),
                    null,
                    () -> remove(capturedIndex)
            ));
        }

        addButton(4, 4, 40, KineticText.translatable("gui.kineticcore.config.back"), null, this::onClose);
        if (!fixedItems) addButton(596 - 4 - 56, 4, 56, KineticText.translatable("gui.kineticcore.config.add"), null,
                itemValues ? this::addItem : this::add);
        addButton(596, 4, 40, KineticText.translatable("gui.done"), null, this::finish);
    }

    private double listPixelOffset() {
        return listScroll.smoothOffset() * rowHeight;
    }

    private void addListScrollableWidget(AbstractWidget widget) {
        listWidgets.add(widget);
        if (!(widget instanceof dev.xyat.kineticcore.internal.client.gui.widget.InternalControl control)) {
            throw new IllegalArgumentException("Scrollable widget must be an API-created control");
        }
        addScrollableWidget(control,
                LIST_X,
                LIST_Y,
                SCROLL_X - 2,
                LIST_Y + listHeight,
                this::listPixelOffset
        );
    }

    private void setListWidgetsVisible(boolean visible) {
        for (AbstractWidget widget : listWidgets) {
            widget.visible = visible;
        }
    }

    private void add() {
        values.add(integerList ? "0" : "");
        listScroll.update(values.size(), visibleRows);
        listScroll.setOffset(Math.max(0, values.size() - visibleRows));
        status = null;
        rebuildUi();
    }

    // "itemId|number": the item part and the number part of one row.
    private static String itemValueId(String row) {
        int bar = row.lastIndexOf('|');
        return (bar < 0 ? row : row.substring(0, bar)).trim();
    }

    private static String itemValueNumber(String row) {
        int bar = row.lastIndexOf('|');
        return bar < 0 ? "1" : row.substring(bar + 1).trim();
    }

    private static net.minecraft.world.item.ItemStack itemValueStack(String row) {
        var id = dev.xyat.kineticcore.api.resource.KineticResourceIds.tryParse(itemValueId(row));
        var item = id == null ? null : dev.xyat.kineticcore.api.registry.KineticRegistries.items().get(id);
        return item == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(item);
    }

    private void addItem() {
        dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.openItemSelector(selection -> {
            if (selection == null || !selection.isItem()) return;
            var id = dev.xyat.kineticcore.api.registry.KineticRegistries.items().id(selection.stack().getItem());
            if (id == null) return;
            values.add(id + "|1");
            listScroll.update(values.size(), visibleRows);
            listScroll.setOffset(Math.max(0, values.size() - visibleRows));
            status = null;
            rebuildUi();
        });
    }

    private void changeItem(int index) {
        dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.openItemSelector(selection -> {
            if (selection == null || !selection.isItem() || index < 0 || index >= values.size()) return;
            var id = dev.xyat.kineticcore.api.registry.KineticRegistries.items().id(selection.stack().getItem());
            if (id == null) return;
            values.set(index, id + "|" + itemValueNumber(values.get(index)));
            rebuildUi();
        });
    }

    private void remove(int index) {
        if (index >= 0 && index < values.size()) values.remove(index);
        clearDragState();
        listScroll.update(values.size(), visibleRows);
        status = null;
        rebuildUi();
    }

    private void move(int index, int direction) {
        int target = index + direction;
        if (index < 0 || index >= values.size() || target < 0 || target >= values.size()) return;
        moveTo(index, target);
    }

    private void moveTo(int index, int target) {
        if (index < 0 || index >= values.size() || target < 0 || target >= values.size() || index == target) return;
        String value = values.remove(index);
        values.add(target, value);
        ensureVisible(target);
        status = null;
        rebuildUi();
    }

    private void ensureVisible(int index) {
        listScroll.update(values.size(), visibleRows);
        if (index < listScroll.offset()) {
            listScroll.setOffset(index);
        } else if (index >= listScroll.offset() + visibleRows) {
            listScroll.setOffset(index - visibleRows + 1);
        }
    }

    private int rowAt(double mouseY) {
        if (values.isEmpty()) return -1;
        double clampedY = Math.max(LIST_Y, Math.min(mouseY, LIST_Y + listHeight - 1));
        int index = (int) Math.floor((clampedY - LIST_Y + listPixelOffset()) / rowHeight);
        return Math.max(0, Math.min(index, values.size() - 1));
    }

    private boolean inList(double mouseX, double mouseY) {
        return mouseX >= LIST_X && mouseX < ROW_RIGHT
                && mouseY >= LIST_Y && mouseY < LIST_Y + listHeight;
    }

    private void updateDragTarget(double mouseX, double mouseY) {
        if (draggingIndex < 0) return;

        dragMouseX = mouseX;
        dragMouseY = mouseY;
        long now = System.nanoTime();
        if (now - lastDragScrollNanos >= DRAG_SCROLL_INTERVAL_NANOS) {
            if (mouseY <= LIST_Y + 10 && listScroll.offset() > 0) {
                listScroll.setOffset(listScroll.offset() - 1);
                lastDragScrollNanos = now;
            } else if (mouseY >= LIST_Y + listHeight - 10 && listScroll.offset() < listScroll.maxOffset()) {
                listScroll.setOffset(listScroll.offset() + 1);
                lastDragScrollNanos = now;
            }
        }
        double ghostCenterY = mouseY - dragGrabOffsetY + rowHeight / 2.0;
        dragTargetIndex = rowAt(ghostCenterY);
    }

    private int previewPosition(int index) {
        if (index == draggingIndex) return dragTargetIndex;
        if (draggingIndex < dragTargetIndex && index > draggingIndex && index <= dragTargetIndex) {
            return index - 1;
        }
        if (draggingIndex > dragTargetIndex && index >= dragTargetIndex && index < draggingIndex) {
            return index + 1;
        }
        return index;
    }

    private void clearDragState() {
        setListWidgetsVisible(true);
        draggingIndex = -1;
        dragTargetIndex = -1;
        dragMouseX = 0.0;
        dragMouseY = 0.0;
        dragGrabOffsetX = 0.0;
        dragGrabOffsetY = 0.0;
        lastDragScrollNanos = 0L;
    }

    private void finish() {
        if (!integerList) {
            resultConsumer.accept(new ArrayList<>(values));
            navigateBack();
            return;
        }

        List<Integer> parsed = new ArrayList<>(values.size());
        for (String value : values) {
            try {
                parsed.add(Integer.parseInt(value.trim()));
            } catch (NumberFormatException ignored) {
                status = KineticText.translatable("gui.kineticcore.config.list_invalid");
                return;
            }
        }
        resultConsumer.accept(parsed);
        navigateBack();
    }

    private void renderSnapshotButton(GuiGraphics graphics, int x, int y, int width, Component label, boolean lifted) {
        GuiTheme.stateSurface(graphics, x, y, width, STANDARD_CONTROL_HEIGHT,
                KineticTheme.Surface.PANEL_ALT, false, lifted, false);
        VanillaGuiDraw.centeredText(graphics, font, label, x + width / 2, y + 4, GuiTheme.current().text());
    }

    private void renderRowSnapshot(
            GuiGraphics graphics,
            int valueIndex,
            int rank,
            int rowX,
            int y,
            boolean lifted
    ) {
        int dx = rowX - LIST_X;
        int rowWidth = ROW_RIGHT - LIST_X - 4;
        GuiTheme.stateSurface(graphics, rowX + 2, y + 1, rowWidth, rowHeight - 2,
                lifted ? KineticTheme.Surface.PANEL_ALT : KineticTheme.Surface.PANEL, false, lifted, false);
        VanillaGuiDraw.text(graphics, 
                font,
                Integer.toString(rank + 1),
                NUMBER_X + dx,
                y + 6,
                rank == 0 ? GuiTheme.current().accentHover() : GuiTheme.current().mutedText(),
                false
        );
        GuiTheme.stateSurface(graphics, EDIT_X + dx, y + 2, EDIT_WIDTH, STANDARD_CONTROL_HEIGHT,
                KineticTheme.Surface.FIELD, false, lifted, false);
        KineticText.drawScrollingLeft(
                graphics,
                font,
                Component.literal(values.get(valueIndex)),
                EDIT_X + dx + 4,
                y + 6,
                EDIT_WIDTH - 8,
                GuiTheme.fieldText(),
                false
        );
        renderSnapshotButton(
                graphics, UP_X + dx, y + 2, 32,
                KineticText.translatable("gui.kineticcore.config.move_up"), lifted
        );
        renderSnapshotButton(
                graphics, DOWN_X + dx, y + 2, 32,
                KineticText.translatable("gui.kineticcore.config.move_down"), lifted
        );
        renderSnapshotButton(
                graphics, REMOVE_X + dx, y + 2, 34,
                KineticText.translatable("gui.kineticcore.config.remove_short"), lifted
        );
    }

    private void renderDragPreview(GuiGraphics graphics) {
        if (draggingIndex < 0 || dragTargetIndex < 0) return;
        double pixelOffset = listPixelOffset();
        int offsetPixels = (int) Math.round(pixelOffset);

        enableUiScissor(graphics, LIST_X, LIST_Y, SCROLL_X - 2, LIST_Y + listHeight);
        try {
            for (int index = 0; index < values.size(); index++) {
                if (index == draggingIndex) continue;
                int previewIndex = previewPosition(index);
                int y = LIST_Y + previewIndex * rowHeight - offsetPixels;
                if (y + rowHeight <= LIST_Y || y >= LIST_Y + listHeight) continue;
                renderRowSnapshot(graphics, index, previewIndex, LIST_X, y, false);
            }

            int gapY = LIST_Y + dragTargetIndex * rowHeight - offsetPixels;
            if (gapY + rowHeight > LIST_Y && gapY < LIST_Y + listHeight) {
                GuiTheme.stateSurface(graphics, LIST_X + 2, gapY + 1,
                        ROW_RIGHT - LIST_X - 4, rowHeight - 2,
                        KineticTheme.Surface.FIELD, true, false, false);
            }
        } finally {
            disableUiScissor(graphics);
        }
    }

    @Override
    protected void renderCanvasBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GuiTheme.panel(graphics, 0, 0, canvasWidth(), canvasHeight());
        dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter.wrap(graphics)
                .scrollingTextCentered(title, canvasWidth() / 2, 8, canvasWidth() - 2 * (4 + 56 + 4 + 40 + 4), GuiTheme.current().accentHover(), true);
        GuiTheme.separator(graphics, 4, 24, canvasWidth() - 8);

        if (description != null) {
            dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter.wrap(graphics)
                    .scrollingText(description, 8, 30, canvasWidth() - 16, GuiTheme.current().text(), false);
        }

        if (draggingIndex >= 0) {
            renderDragPreview(graphics);
        } else {
            double pixelOffset = listPixelOffset();
            enableUiScissor(graphics, LIST_X, LIST_Y, SCROLL_X - 2, LIST_Y + listHeight);
            try {
                int first = Math.max(0, (int) Math.floor(listScroll.smoothOffset()));
                int last = Math.min(first + visibleRows + 2, values.size());
                for (int index = first; index < last; index++) {
                    int y = LIST_Y + index * rowHeight - (int) Math.round(pixelOffset);
                    VanillaGuiDraw.text(graphics, 
                            font,
                            Integer.toString(index + 1),
                            NUMBER_X,
                            y + 6,
                            index == 0 ? GuiTheme.current().accentHover() : GuiTheme.current().mutedText(),
                            false
                    );
                    if (itemValues) {
                        String row = values.get(index);
                        var stack = itemValueStack(row);
                        int slotY = y + 1;
                        GuiTheme.itemSlot(graphics, EDIT_X, slotY, 22, mouseX >= EDIT_X && mouseX < EDIT_X + 22
                                && mouseY >= Math.max(slotY, LIST_Y) && mouseY < Math.min(slotY + 22, LIST_Y + listHeight));
                        GuiTheme.item(graphics, font, stack, EDIT_X, slotY, 22, 1.0F, false);
                        Component name = stack.isEmpty()
                                ? Component.literal(itemValueId(row)).withStyle(net.minecraft.ChatFormatting.RED)
                                : stack.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.GOLD)
                                        .append(Component.literal("  " + itemValueId(row)).withStyle(net.minecraft.ChatFormatting.GRAY));
                        KineticText.drawScrollingLeft(graphics, font, name, EDIT_X + 28, y + 8,
                                VALUE_X - 4 - (EDIT_X + 28), GuiTheme.current().text(), false);
                    }
                    if (fixedItems) {
                        GuiTheme.stateSurface(graphics, EDIT_X, y + 2, EDIT_WIDTH, STANDARD_CONTROL_HEIGHT,
                                KineticTheme.Surface.PANEL_ALT, false, false, false);
                        KineticText.drawScrollingLeft(graphics, font, Component.literal(values.get(index)),
                                EDIT_X + 4, y + 6, EDIT_WIDTH - 8, GuiTheme.fieldText(), false);
                    }
                }
            } finally {
                disableUiScissor(graphics);
            }
        }

        if (values.isEmpty()) {
            VanillaGuiDraw.centeredText(graphics, font, KineticText.translatable("gui.kineticcore.config.list_empty"),
                    canvasWidth() / 2, LIST_Y + listHeight / 2 - 4, GuiTheme.current().mutedText());
        }
        listScroll.render(
                graphics, mouseX, mouseY,
                SCROLL_X, LIST_Y, SCROLL_WIDTH, listHeight, 18
        );
        if (status != null) {
            VanillaGuiDraw.centeredText(graphics, font, status, canvasWidth() / 2, 330, GuiTheme.current().danger());
        }
    }

    @Override
    protected void renderCanvasForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (draggingIndex < 0 || draggingIndex >= values.size()) return;

        int ghostX = (int) Math.round(dragMouseX - dragGrabOffsetX);
        ghostX = Math.max(LIST_X, Math.min(12, ghostX));
        int ghostY = (int) Math.round(dragMouseY - dragGrabOffsetY);
        ghostY = Math.max(LIST_Y - rowHeight, Math.min(LIST_Y + listHeight - rowHeight, ghostY));
        int rowWidth = ROW_RIGHT - LIST_X - 4;

        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, 0.0F, 0.0F, 250.0F);
        try {
            graphics.fill(
                    ghostX + 5,
                    ghostY + 5,
                    ghostX + 2 + rowWidth + 5,
                    ghostY + rowHeight + 3,
                    GuiTheme.current().shadow()
            );
            renderRowSnapshot(graphics, draggingIndex, dragTargetIndex, ghostX, ghostY, true);
        } finally {
            VanillaGuiDraw.pop(graphics);
        }
    }

    @Override
    protected boolean canvasMouseClicked(double mouseX, double mouseY, int button) {
        if (itemValues && button == 0 && draggingIndex < 0 && mouseX >= EDIT_X && mouseX < EDIT_X + 22 && inList(mouseX, mouseY)) {
            int index = rowAt(mouseY);
            int slotY = LIST_Y + index * rowHeight - (int) Math.round(listPixelOffset()) + 1;
            if (index >= 0 && mouseY >= slotY && mouseY < slotY + 22) {
                changeItem(index);
                return true;
            }
        }
        if (button == 0 && listScroll.beginDrag(
                mouseX, mouseY, SCROLL_X, LIST_Y, SCROLL_WIDTH, listHeight, 18, 2)) {
            return true;
        }
        if (button == 0 && KineticClientRuntime.controlModifierDown() && inList(mouseX, mouseY)) {
            int index = rowAt(mouseY);
            if (index >= 0) {
                int sourceY = LIST_Y + index * rowHeight - (int) Math.round(listPixelOffset());
                draggingIndex = index;
                dragTargetIndex = index;
                dragMouseX = mouseX;
                dragMouseY = mouseY;
                dragGrabOffsetX = mouseX - LIST_X;
                dragGrabOffsetY = mouseY - sourceY;
                lastDragScrollNanos = System.nanoTime();
                clearControlFocus();
                setListWidgetsVisible(false);
                return true;
            }
        }
        return super.canvasMouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseDragged(
            double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingIndex >= 0) {
            updateDragTarget(mouseX, mouseY);
            return true;
        }
        if (listScroll.drag(mouseY, LIST_Y, listHeight, 18)) {
            return true;
        }
        return super.canvasMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean canvasMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingIndex >= 0) {
            int from = draggingIndex;
            int target = dragTargetIndex;
            clearDragState();
            if (target >= 0 && target != from) {
                moveTo(from, target);
            }
            return true;
        }
        return listScroll.release(button)
                || super.canvasMouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected boolean canvasMouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= LIST_X && mouseX <= SCROLL_X - 4
                && mouseY >= LIST_Y && mouseY < LIST_Y + listHeight
                && listScroll.scroll(delta, 1.0D)) {
            return true;
        }
        return super.canvasMouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean handleCloseRequest() {
        clearDragState();
        if (values.equals(originalValues)) {
            return false;
        }

        openDialog(
                KineticText.translatable("gui.kineticcore.config.unsaved_action.title"),
                KineticText.translatable("gui.kineticcore.config.unsaved_list.message"),
                KineticText.translatable("gui.yes"),
                KineticText.translatable("gui.no"),
                this::finish,
                this::navigateBack
        );
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
