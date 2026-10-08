package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.render.VanillaGuiDraw;

import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridOutline;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemGrid;
import dev.xyat.kineticcore.internal.client.gui.theme.GuiTheme;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import dev.xyat.kineticcore.internal.client.render.KineticRenderRuntime;
import dev.xyat.kineticcore.internal.client.widget.KineticControlBridge;
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
public final class ItemGridWidget extends VerticalScrollListWidget implements KineticItemGrid {

    private final Font font;
    private final ItemGridDensity density;
    private final Consumer<Integer> responder;
    private final int zLevel;
    private List<ItemGridItem> items = List.of();
    private int hoveredIndex = -1;
    private int selectedIndex = -1;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public ItemGridWidget(
            FactoryAccess access,
            Font font,
            int x,
            int y,
            int width,
            int height,
            ItemGridDensity density,
            List<? extends ItemGridItem> items,
            int initialScrollOffset,
            Consumer<Integer> responder,
            int zLevel
    ) {
        super(access, x, y, width, height, initialScrollOffset);
        this.font = font;
        this.density = density == null ? ItemGridDensity.STANDARD : density;
        this.responder = responder;
        this.zLevel = zLevel;
        setItems(items);
    }

    @Override
    public void setItems(List<? extends ItemGridItem> nextItems) {
        items = nextItems == null ? List.of() : List.copyOf(nextItems);
        if (selectedIndex < 0 || selectedIndex >= items.size()
                || !items.get(selectedIndex).selected()) {
            int modelSelectedIndex = findSelectedItem();
            if (modelSelectedIndex >= 0) selectedIndex = modelSelectedIndex;
            else if (selectedIndex >= items.size()) selectedIndex = -1;
        }
        refreshRange();
    }

    @Override
    public List<ItemGridItem> items() {
        return List.copyOf(items);
    }

    @Override
    public int columns() {
        return gridLayout().columns();
    }

    @Override
    public int visibleRows() {
        return gridLayout().visibleRows();
    }

    @Override
    public int itemAt(double mouseX, double mouseY) {
        return visible ? gridLayout().itemAt(mouseX, mouseY, scroll.smoothIndexOffset(),
                scroll.visualShift(density.cellPitch()), items.size()) : -1;
    }

    @Override
    public ItemStack stackAt(double mouseX, double mouseY) {
        int index = itemAt(mouseX, mouseY);
        return index >= 0 ? safeStack(items.get(index)) : ItemStack.EMPTY;
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
        VanillaGuiDraw.push(graphics);
        VanillaGuiDraw.translate(graphics, 0, 0, zLevel);
        try {
            ItemGridLayout layout = gridLayout();
            KineticRenderRuntime.enableScissor(graphics, layout.clipLeft(), layout.clipTop(), layout.clipRight(), layout.clipBottom());
            try {
                int cols = columns();
                int startRow = scroll.smoothIndexOffset();
                int shift = scroll.visualShift(density.cellPitch());
                int start = startRow * cols;
                int end = layout.renderEndIndex(startRow, shift, items.size());
                for (int index = start; index < end; index++) {
                    int x = layout.slotX(index);
                    int y = layout.slotY(index, startRow, shift);
                    if (!layout.rowIntersects(index, startRow, shift)) continue;
                    ItemGridItem item = items.get(index);
                    if (item == null) continue;
                    boolean hover = layout.insideClip(mouseX, mouseY)
                            && GuiTheme.hovering(mouseX, mouseY, x, y, density.slotSize(), density.slotSize());
                    GuiTheme.itemSlot(
                            graphics, x, y, density.slotSize(), density.slotSize(), 2,
                            false, false, false
                    );
                    ItemStack stack = safeStack(item);
                    if (!stack.isEmpty()) {
                        GuiTheme.item(graphics, font, stack, x, y, density.slotSize(), density.renderScale(), density.decorations());
                    }
                    KineticTheme.Indicator outline = outlineIndicator(item.outline());
                    if (item.error()) {
                        GuiTheme.indicatorOutline(
                                graphics, x, y, density.slotSize(), density.slotSize(), KineticTheme.Indicator.DANGER
                        );
                    } else if (hover) {
                        GuiTheme.stateOutline(
                                graphics, x, y, density.slotSize(), density.slotSize(), false, true, false
                        );
                    } else if (item.selected()) {
                        GuiTheme.stateOutline(
                                graphics, x, y, density.slotSize(), density.slotSize(), true, false, false
                        );
                    } else if (outline != null) {
                        GuiTheme.indicatorOutline(
                                graphics, x, y, density.slotSize(), density.slotSize(), outline
                        );
                    }
                    scroll.renderSelectionFlash(graphics, index, x, y, density.slotSize(), density.slotSize());
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible) return false;
        if (handleScrollbarPress(mouseX, mouseY, button, selectedIndex)) return true;
        if (button != 0) return false;
        int index = itemAt(mouseX, mouseY);
        if (index < 0 || index >= items.size()) return false;
        ItemGridItem item = items.get(index);
        if (item == null || !item.active()) return false;
        selectedIndex = index;
        if (responder != null) responder.accept(index);
        return true;
    }

    @Override
    protected void updateWidgetNarration(@Nonnull NarrationElementOutput output) {
        if (hoveredIndex < 0 || hoveredIndex >= items.size()) return;
        ItemStack stack = safeStack(items.get(hoveredIndex));
        if (!stack.isEmpty()) output.add(NarratedElementType.TITLE, stack.getHoverName());
    }

    private ItemGridLayout gridLayout() {
        return new ItemGridLayout(getX(), getY(), contentWidth(), getHeight(), density);
    }

    private int findSelectedItem() {
        for (int index = items.size() - 1; index >= 0; index--) {
            ItemGridItem item = items.get(index);
            if (item != null && item.selected()) return index;
        }
        return -1;
    }

    /** Theme indicator for a business outline, or {@code null} when the slot keeps its default outline. */
    private static KineticTheme.Indicator outlineIndicator(ItemGridOutline outline) {
        if (outline == null) return null;
        return switch (outline) {
            case NONE -> null;
            case WARNING -> KineticTheme.Indicator.WARNING;
            case SUCCESS -> KineticTheme.Indicator.SUCCESS;
        };
    }

    private static ItemStack safeStack(ItemGridItem item) {
        return item == null || item.stack() == null ? ItemStack.EMPTY : item.stack();
    }

    @Override
    protected int scrollRangeRows() {
        return (items.size() + columns() - 1) / columns();
    }

    @Override
    protected int rowPitch() {
        return density.cellPitch();
    }

    @Override
    protected int selectionTargetOffset(int selected) {
        return Math.max(0, selected / columns() - visibleRows() / 2);
    }
}
