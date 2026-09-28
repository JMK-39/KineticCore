package dev.xyat.kineticcore.api.client.gui.widget.list;

import dev.xyat.kineticcore.api.client.gui.input.KeyInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseDragInput;
import dev.xyat.kineticcore.api.client.gui.input.MouseInput;
import dev.xyat.kineticcore.api.client.gui.input.ScrollInput;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.scroll.KineticScrollController;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.api.client.gui.widget.KineticCustomControl;
import dev.xyat.kineticcore.api.client.input.KineticKeyBindings;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 自绘行列表基类：平滑滚动、主题滚动条、选中项中键跳转闪烁、键盘上下选择都由核心处理，附属只绘制每一行。
 * 取代旧的 {@code SmoothSelectionList}/{@code SmoothEntry}；通过 {@code ui.add(list)} 挂到页面。
 * Base class for self-drawn row lists. Smooth scrolling, the themed scrollbar, middle-click jump/flash to the
 * selection and arrow-key selection are handled by the core; subclasses only draw rows. Replaces the old
 * {@code SmoothSelectionList}/{@code SmoothEntry}; attach it to a page with {@code ui.add(list)}.
 *
 * @param <T> 行数据类型 / row data type
 */
public abstract class KineticRowList<T> extends KineticCustomControl {
    /** 滚动条宽度 / Scrollbar width. */
    public static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 2;
    private static final int MIN_THUMB = 16;

    private final int rowHeight;
    private final List<T> items = new ArrayList<>();
    private final List<T> itemsView = Collections.unmodifiableList(items);
    private final KineticScrollController scroll = new KineticScrollController();
    private int selectedIndex = -1;
    private int lastClickedIndex = -1;
    private int hoveredIndex = -1;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private Consumer<Integer> onSelect;

    /** 创建行列表 / Creates a row list. */
    protected KineticRowList(int x, int y, int width, int height, int rowHeight) {
        super(x, y, width, height);
        this.rowHeight = Math.max(1, rowHeight);
        // 中键跳转目标：选中行；没有选中行时为最近一次点击的行（与原版列表“点击即选中”一致）。
        // Middle-click target: the selected row, or the last clicked row when nothing is selected (vanilla lists
        // selected a row on click).
        scroll.bindSelection(this::jumpTargetIndex);
        setTooltip(() -> hoveredIndex >= 0 && hoveredIndex < items.size()
                ? rowTooltip(items.get(hoveredIndex), hoveredIndex) : null);
    }

    // ---------------------------------------------------------------- data

    /** 每行像素高度 / Row height in pixels. */
    public final int rowHeight() {
        return rowHeight;
    }

    /** 只读行数据 / Read-only rows. */
    public final List<T> items() {
        return itemsView;
    }

    /** 替换全部行；选中下标越界时清除 / Replaces all rows; an out-of-range selection is cleared. */
    public final void setItems(Collection<? extends T> rows) {
        items.clear();
        if (rows != null) items.addAll(rows);
        if (selectedIndex >= items.size()) selectedIndex = -1;
        if (lastClickedIndex >= items.size()) lastClickedIndex = -1;
        scroll.update(items.size(), scrollRangeRows());
    }

    /** 当前选中下标，无则 -1 / Selected index, or -1. */
    public final int selectedIndex() {
        return selectedIndex;
    }

    /** 当前选中行，无则 null / Selected row, or null. */
    public final T selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    /** 设置选中下标（不触发回调）/ Sets the selection without firing the callback. */
    public final void setSelectedIndex(int index) {
        selectedIndex = index >= 0 && index < items.size() ? index : -1;
    }

    /** 用户改变选中项时回调 / Callback fired when the user changes the selection. */
    public final void setOnSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
    }

    /** 选中并触发回调 / Selects an index and fires the callback. */
    public final void select(int index) {
        int next = index >= 0 && index < items.size() ? index : -1;
        selectedIndex = next;
        scrollTo(next);
        if (onSelect != null) onSelect.accept(next);
    }

    /** 中键跳转目标行 / Row targeted by the middle-click jump. */
    private int jumpTargetIndex() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) return selectedIndex;
        return lastClickedIndex >= 0 && lastClickedIndex < items.size() ? lastClickedIndex : -1;
    }

    /** 当前行偏移 / Current row offset. */
    public final int scrollOffset() {
        return scroll.offset();
    }

    /** 立即滚动到行偏移 / Jumps to a row offset. */
    public final void setScrollOffset(int rows) {
        scroll.update(items.size(), scrollRangeRows());
        scroll.setOffset(rows);
    }

    /** 确保该行可见 / Scrolls just enough to make the row visible. */
    public final void scrollTo(int index) {
        if (index < 0 || index >= items.size()) return;
        int visible = visibleRows();
        int offset = scroll.offset();
        if (index < offset) setScrollOffset(index);
        else if (index >= offset + visible) setScrollOffset(index - visible + 1);
    }

    /** 完整显示的行数 / Number of fully visible rows. */
    public final int visibleRows() {
        return Math.max(1, controlHeight() / rowHeight);
    }

    /** Include a partially visible row when limiting the final scroll position. */
    private int scrollRangeRows() {
        return Math.max(1, (controlHeight() + rowHeight - 1) / rowHeight);
    }

    /** 鼠标下的行，无则 -1 / Row under the pointer, or -1. */
    public final int rowAt(double mouseX, double mouseY) {
        if (mouseX < controlX() || mouseX >= controlX() + rowsWidth() || mouseY < controlY() || mouseY >= controlY() + controlHeight()) {
            return -1;
        }
        int index = scroll.smoothIndexOffset() + (int) ((mouseY - controlY() + scroll.visualShift(rowHeight)) / rowHeight);
        return index >= 0 && index < items.size() ? index : -1;
    }

    /** 某行当前的顶部 Y（含平滑滚动位移）/ Current top Y of a row, including the smooth scroll shift. */
    public final int rowTop(int index) {
        return controlY() + (index - scroll.smoothIndexOffset()) * rowHeight - scroll.visualShift(rowHeight);
    }

    /** 最近一次绘制时的鼠标 X，供 renderRow 内的悬停判断 / Mouse X of the last render, for hover checks in renderRow. */
    protected final int mouseX() {
        return lastMouseX;
    }

    /** 最近一次绘制时的鼠标 Y / Mouse Y of the last render. */
    protected final int mouseY() {
        return lastMouseY;
    }

    /** 行区域宽度（扣除滚动条）/ Row area width, excluding the scrollbar gutter. */
    public final int rowsWidth() {
        return scroll.canScroll() ? Math.max(1, controlWidth() - SCROLLBAR_WIDTH - SCROLLBAR_GAP) : controlWidth();
    }

    // ---------------------------------------------------------------- subclass hooks

    /**
     * 绘制一行内容；背景与选中闪烁已由 {@link #renderRowBackground} 绘制。
     * Draws one row's content; the background and selection flash are already drawn.
     */
    protected abstract void renderRow(KineticGraphics graphics, T item, int index, int x, int y, int width, int height,
                                      boolean hovered, boolean selected);

    /** 行背景，默认主题斑马纹 / Row background; defaults to themed zebra surfaces. */
    protected void renderRowBackground(KineticGraphics graphics, int index, int x, int y, int width, int height,
                                       boolean hovered, boolean selected) {
        KineticTheme.stateSurface(graphics, x, y + 1, width, height - 2,
                index % 2 == 0 ? KineticTheme.Surface.PANEL_ALT : KineticTheme.Surface.PANEL, selected, hovered, false);
    }

    /** 行点击；默认左键选中 / Row click; selects on left click by default. */
    protected boolean onRowClick(T item, int index, MouseInput input) {
        if (!input.isLeft()) return false;
        select(index);
        return true;
    }

    /** 悬停行的提示，默认无 / Tooltip for the hovered row; none by default. */
    protected Component rowTooltip(T item, int index) {
        return null;
    }

    /** 空列表时居中显示的文字，默认无 / Text shown centered when empty; none by default. */
    protected Component emptyText() {
        return null;
    }

    // ---------------------------------------------------------------- control plumbing

    @Override
    protected final void render(KineticGraphics graphics, int mouseX, int mouseY, float partialTick) {
        scroll.update(items.size(), scrollRangeRows());
        int x = controlX();
        int y = controlY();
        int width = rowsWidth();
        int height = controlHeight();
        hoveredIndex = controlHovered() ? rowAt(mouseX, mouseY) : -1;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (items.isEmpty()) {
            Component empty = emptyText();
            if (empty != null) {
                graphics.centeredText(empty, x + width / 2, y + (height - graphics.lineHeight()) / 2,
                        KineticTheme.current().mutedText(), false);
            }
        } else {
            int first = scroll.smoothIndexOffset();
            int shift = scroll.visualShift(rowHeight);
            int last = Math.min(items.size(), first + visibleRows() + 2);
            graphics.clipped(x, y, x + width, y + height, () -> {
                for (int index = first; index < last; index++) {
                    int rowY = y + (index - first) * rowHeight - shift;
                    boolean selected = index == selectedIndex;
                    boolean hovered = index == hoveredIndex;
                    renderRowBackground(graphics, index, x, rowY, width, rowHeight, hovered, selected);
                    renderRow(graphics, items.get(index), index, x, rowY, width, rowHeight, hovered, selected);
                    // 橘黄色边框闪烁画在行内容之上 / The orange border flash is drawn on top of the row content.
                    scroll.renderSelectionFlash(graphics, index, x, rowY + 1, width, rowHeight - 2);
                }
            });
        }
        if (scroll.canScroll()) {
            scroll.render(graphics, mouseX, mouseY, scrollbarX(), y, SCROLLBAR_WIDTH, height, MIN_THUMB);
        }
    }

    @Override
    protected final boolean onMouseClick(MouseInput input) {
        if (scroll.canScroll() && scroll.beginDrag(input.x(), input.y(), input.button(),
                scrollbarX(), controlY(), SCROLLBAR_WIDTH, controlHeight(), MIN_THUMB)) {
            return true;
        }
        int index = rowAt(input.x(), input.y());
        if (index < 0 || !onRowClick(items.get(index), index, input)) return false;
        lastClickedIndex = index;
        return true;
    }

    @Override
    protected final boolean onMouseDrag(MouseDragInput input) {
        return scroll.drag(input.y(), controlY(), controlHeight(), MIN_THUMB);
    }

    @Override
    protected final boolean onMouseRelease(MouseInput input) {
        return scroll.release(input.button());
    }

    @Override
    protected final boolean onMouseScroll(ScrollInput input) {
        return scroll.scroll(input.deltaY());
    }

    @Override
    protected boolean isFocusable() {
        return true;
    }

    @Override
    protected boolean onKeyPress(KeyInput input) {
        if (items.isEmpty()) return false;
        if (input.is(KineticKeyBindings.Key.UP)) {
            select(selectedIndex <= 0 ? 0 : selectedIndex - 1);
            return true;
        }
        if (input.is(KineticKeyBindings.Key.DOWN)) {
            select(Math.min(items.size() - 1, selectedIndex + 1));
            return true;
        }
        return false;
    }

    private int scrollbarX() {
        return controlX() + controlWidth() - SCROLLBAR_WIDTH;
    }
}
