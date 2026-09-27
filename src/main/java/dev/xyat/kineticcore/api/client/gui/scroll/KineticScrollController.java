package dev.xyat.kineticcore.api.client.gui.scroll;

import dev.xyat.kineticcore.api.client.gui.input.MouseButton;
import dev.xyat.kineticcore.api.client.gui.render.KineticGraphics;
import dev.xyat.kineticcore.api.client.gui.theme.KineticTheme;
import dev.xyat.kineticcore.internal.client.gui.render.GuiGraphicsAdapter;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.GridScrollController;

import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * 附属自绘列表、网格与横向条带使用的标准滚动控制器：逻辑偏移、平滑动画、拖拽、滚轮与主题滚动条。
 * 绑定选中项后，所属页面会自动提供 0.5 秒悬停提示与中键跳回选中项（选中项闪烁两次）。
 * Standard scroll controller for addon-drawn lists, grids and horizontal strips: logical offset, smooth
 * animation, dragging, wheel and the themed scrollbar. Once a selection is bound, the owning page provides the
 * 0.5 s hover hint and the middle-click jump back to the selection (which then flashes twice) automatically.
 */
public final class KineticScrollController {
    private final GridScrollController delegate = new GridScrollController();

    /** 创建控制器 / Creates a controller. */
    public KineticScrollController() {
    }

    /**
     * 绑定当前选中项（-1 表示无），默认跳转偏移让选中项居中。
     * Binds the selected index (-1 for none); the default jump offset centres the selection.
     */
    public KineticScrollController bindSelection(IntSupplier selectedIndex) {
        delegate.bindSelection(selectedIndex);
        return this;
    }

    /**
     * 绑定选中项并自定义“选中下标 → 滚动偏移”换算（网格用 {@code i -> i / columns - visibleRows / 2}）。
     * Binds the selection with a custom index-to-offset mapping (grids: {@code i -> i / columns - visibleRows / 2}).
     */
    public KineticScrollController bindSelection(IntSupplier selectedIndex, IntUnaryOperator targetOffset) {
        delegate.bindSelection(selectedIndex, targetOffset);
        return this;
    }

    /** 解除选中项绑定 / Removes the selection binding. */
    public KineticScrollController unbindSelection() {
        delegate.unbindSelection();
        return this;
    }

    /** 更新条目总数与可见条目数 / Updates the total and visible item counts. */
    public void update(int totalItems, int visibleItems) {
        delegate.update(totalItems, visibleItems);
    }

    /** 显式指定最大偏移，同时保留总数/可见数用于滑块尺寸 / Sets an explicit maximum offset, keeping counts for thumb sizing. */
    public void updateRange(int maxOffset, int totalItems, int visibleItems) {
        delegate.updateRange(maxOffset, totalItems, visibleItems);
    }

    /** 当前整数逻辑偏移 / Current integer logical offset. */
    public int offset() {
        return delegate.offset();
    }

    /** 当前平滑小数偏移 / Current smooth fractional offset. */
    public double smoothOffset() {
        return delegate.smoothOffset();
    }

    /** 平滑偏移对应的首个条目下标 / First item index addressed by the smooth offset. */
    public int smoothIndexOffset() {
        return delegate.smoothIndexOffset();
    }

    /** 平滑偏移的小数部分 / Fractional part of the smooth offset. */
    public double fractionalOffset() {
        return delegate.fractionalOffset();
    }

    /** 小数偏移换算成的像素位移 / Pixel shift of the fractional offset for one row size. */
    public int visualShift(int unitPixels) {
        return delegate.visualShift(unitPixels);
    }

    /** 最大逻辑偏移 / Maximum logical offset. */
    public int maxOffset() {
        return delegate.maxOffset();
    }

    /** 内容是否超出可滚动 / Whether the content can scroll. */
    public boolean canScroll() {
        return delegate.canScroll();
    }

    /** 立即跳到偏移 / Jumps to an offset immediately. */
    public void setOffset(int offset) {
        delegate.setOffset(offset);
    }

    /** 平滑滚动到偏移（与滚轮相同的缓动）/ Smoothly scrolls to an offset (same easing as the wheel). */
    public void scrollTo(int offset) {
        delegate.animateTo(offset);
    }

    /** 回到顶部并结束拖拽 / Resets to the top and ends dragging. */
    public void reset() {
        delegate.reset();
    }

    /** 按一格滚轮滚动；ScrollInput.deltaY 可直接传入 / Scrolls by wheel notches (pass ScrollInput.deltaY). */
    public boolean scroll(double delta) {
        return delegate.scroll(delta);
    }

    /** 以指定步长滚动 / Scrolls with an explicit step. */
    public boolean scroll(double delta, double step) {
        return delegate.scroll(delta, step);
    }

    /** 左键按在纵向轨道或滑块上时开始拖拽 / Starts a vertical drag when the left button hits the track or thumb. */
    public boolean beginDrag(double mouseX, double mouseY, MouseButton button, int x, int y, int width, int height,
                             int minThumbHeight) {
        return button == MouseButton.LEFT && delegate.beginDrag(mouseX, mouseY, x, y, width, height, minThumbHeight, 1);
    }

    /** 纵向拖拽中更新 / Updates an active vertical drag. */
    public boolean drag(double mouseY, int y, int height, int minThumbHeight) {
        return delegate.drag(mouseY, y, height, minThumbHeight);
    }

    /** 左键按在横向轨道或滑块上时开始拖拽 / Starts a horizontal drag when the left button hits the track or thumb. */
    public boolean beginHorizontalDrag(double mouseX, double mouseY, MouseButton button, int x, int y, int width,
                                       int height, int minThumbWidth) {
        return button == MouseButton.LEFT
                && delegate.beginHorizontalDrag(mouseX, mouseY, x, y, width, height, minThumbWidth, 1);
    }

    /** 横向拖拽中更新 / Updates an active horizontal drag. */
    public boolean dragHorizontal(double mouseX, int x, int width, int minThumbWidth) {
        return delegate.dragHorizontal(mouseX, x, width, minThumbWidth);
    }

    /** 松开按键时结束拖拽 / Ends a drag on button release. */
    public boolean release(MouseButton button) {
        return delegate.release(button == MouseButton.LEFT ? 0 : -1);
    }

    /** 绘制纵向主题滚动条（不可滚动时不绘制）/ Renders the vertical themed scrollbar (nothing when not scrollable). */
    public void render(KineticGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height,
                       int minThumbHeight) {
        delegate.render(GuiGraphicsAdapter.unwrap(graphics), mouseX, mouseY, x, y, width, height, minThumbHeight);
    }

    /** 绘制横向主题滚动条 / Renders the horizontal themed scrollbar. */
    public void renderHorizontal(KineticGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height,
                                 int minThumbWidth) {
        delegate.renderHorizontal(GuiGraphicsAdapter.unwrap(graphics), mouseX, mouseY, x, y, width, height, minThumbWidth);
    }

    /** 该下标是否处于闪烁的反色阶段 / Whether the index is in an inverted flash phase. */
    public boolean isSelectionFlashInverted(int selectionIndex) {
        return delegate.isSelectionFlashInverted(selectionIndex);
    }

    /**
     * 该下标处于反色阶段时绘制闪烁覆盖层并返回 true；闪烁时文字请用 {@link KineticTheme#selectionFlashText()}。
     * Draws the flash overlay and returns true while the index is inverted; draw text on top with
     * {@link KineticTheme#selectionFlashText()}.
     */
    public boolean renderSelectionFlash(KineticGraphics graphics, int selectionIndex, int x, int y, int width, int height) {
        return delegate.renderSelectionFlash(GuiGraphicsAdapter.unwrap(graphics), selectionIndex, x, y, width, height);
    }
}
