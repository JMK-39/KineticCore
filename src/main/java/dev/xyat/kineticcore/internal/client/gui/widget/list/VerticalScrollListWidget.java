package dev.xyat.kineticcore.internal.client.gui.widget.list;

import dev.xyat.kineticcore.internal.client.gui.widget.VanillaWidget;

import dev.xyat.kineticcore.internal.client.gui.widget.InternalControl;
import dev.xyat.kineticcore.internal.client.gui.widget.scroll.KineticScroll.GridScrollController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import dev.xyat.kineticcore.internal.client.gui.widget.KineticWidgets.FactoryAccess;
import java.util.Objects;

/**
 * 所有纵向滚动列表/网格共用的滚动条、拖拽、滚轮、中键跳转与悬停提示逻辑。
 * Shared scrollbar, drag, wheel, middle-click jump and hover-hint handling for vertical lists and grids.
 */
public abstract class VerticalScrollListWidget extends VanillaWidget implements InternalControl {
    /** 内容与滚动条间距 / Gap between content and scrollbar. */
    protected static final int SCROLLBAR_GAP = 4;
    /** 滚动条宽度 / Scrollbar width. */
    protected static final int SCROLLBAR_WIDTH = 4;
    /** 滑块最小高度 / Minimum thumb height. */
    protected static final int MIN_THUMB_HEIGHT = 15;

    /** 子控件构造令牌 / Construction token for the child row controls. */
    protected final FactoryAccess factoryAccess;
    /** 共享滚动控制器 / Shared scroll controller. */
    protected final GridScrollController scroll = new GridScrollController();
    /** 首次布局时应用的初始偏移 / Initial offset applied on first layout. */
    protected int pendingInitialScrollOffset;

    /** 统一尺寸下限与初始偏移 / Normalises size and initial offset. */
    protected VerticalScrollListWidget(FactoryAccess access, int x, int y, int width, int height, int initialScrollOffset) {
        super(x, y, Math.max(1, width), Math.max(1, height), Component.empty());
        this.factoryAccess = Objects.requireNonNull(access, "access");
        this.pendingInitialScrollOffset = Math.max(0, initialScrollOffset);
    }

    /** 可滚动的逻辑行数 / Total logical rows. */
    protected abstract int scrollRangeRows();

    /** 单行像素间距 / Pixel pitch of one row. */
    protected abstract int rowPitch();

    /** 当前悬停行的提示，无则 null / Tooltip of the hovered row, or null. */
    protected abstract Component hoveredRowTooltip();

    /** 行按钮位置刷新，网格无需实现 / Row layout refresh; grids need none. */
    protected void refreshLayout() {
    }

    /** 选中项对应的滚动偏移，默认居中 / Offset that centres the selection. */
    protected int selectionTargetOffset(int selected) {
        return Math.max(0, selected - visibleRows() / 2);
    }

    /** 可见行数 / Number of visible rows. */
    protected int visibleRows() {
        return Math.max(1, getHeight() / Math.max(1, rowPitch()));
    }

    /** 行提示优先，否则为滑块中键提示 / Row tooltip first, then the scrollbar middle-click hint. */
    public Component hoveredTooltip() {
        Component row = hoveredRowTooltip();
        return row != null ? row : scroll.hoveredScrollbarTooltip();
    }

    /** 当前滚动偏移 / Current scroll offset. */
    public int scrollOffset() {
        return scroll.offset();
    }

    /** 设置滚动偏移并刷新布局 / Sets the scroll offset and refreshes layout. */
    public void setScrollOffset(int offset) {
        scroll.setOffset(offset);
        refreshLayout();
    }

    /** 最大滚动偏移 / Maximum scroll offset. */
    public int maxScrollOffset() {
        return scroll.maxOffset();
    }

    /** 更新位置尺寸并重算滚动范围 / Updates bounds and recomputes the scroll range. */
    public void setBounds(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        refreshRange();
        refreshLayout();
    }

    /** 按当前内容重算滚动范围 / Recomputes the scroll range from content. */
    protected void refreshRange() {
        scroll.update(scrollRangeRows(), visibleRows());
        if (pendingInitialScrollOffset > 0) {
            scroll.setOffset(pendingInitialScrollOffset);
            pendingInitialScrollOffset = 0;
        }
    }

    /** 去除滚动条后的内容宽度 / Content width excluding the scrollbar. */
    protected int contentWidth() {
        return Math.max(1, getWidth() - SCROLLBAR_GAP - SCROLLBAR_WIDTH);
    }

    /** 滚动条左边 X / Scrollbar left X. */
    protected int scrollbarX() {
        return getX() + getWidth() - SCROLLBAR_WIDTH;
    }

    /** 绘制标准滚动条 / Renders the standard scrollbar. */
    protected final void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!scroll.canScroll()) return;
        scroll.render(graphics, mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT);
    }

    /** 中键跳转或左键开始拖拽滑块时返回 true / True when a middle-click jump or thumb drag starts. */
    protected final boolean handleScrollbarPress(double mouseX, double mouseY, int button, int selected) {
        if (scroll.middleClickThumb(
                mouseX, mouseY, button, false,
                scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT,
                selected, selectionTargetOffset(selected)
        )) {
            refreshLayout();
            return true;
        }
        return button == 0 && scroll.beginDrag(
                mouseX, mouseY, scrollbarX(), getY(), SCROLLBAR_WIDTH, getHeight(), MIN_THUMB_HEIGHT, 2
        );
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scroll.drag(mouseY, getY(), getHeight(), MIN_THUMB_HEIGHT)) {
            refreshLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return scroll.release(button);
    }

    //? if >=1.20.2 {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta) {
    *///?} else {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    //?}
        if (!isMouseOver(mouseX, mouseY) || delta == 0D || !scroll.canScroll()) return false;
        boolean handled = scroll.scroll(delta, 1.0D);
        refreshLayout();
        return handled;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible
                && mouseX >= getX()
                && mouseX < getX() + getWidth()
                && mouseY >= getY()
                && mouseY < getY() + getHeight();
    }
}
