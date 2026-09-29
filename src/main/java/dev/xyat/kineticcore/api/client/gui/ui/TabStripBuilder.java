package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticTabStrip;
import dev.xyat.kineticcore.api.client.gui.widget.TabStripItem;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/** 可滚动变宽标签条 / Scrollable variable-width tab strip. */
public abstract class TabStripBuilder extends ControlBuilder<TabStripBuilder, KineticTabStrip> {
    /** 标签 / Tabs. */
    protected final List<TabStripItem> tabs;
    /** 固定在左侧不滚动的标签数 / Leading tabs pinned outside the scroll area. */
    protected int pinnedLeading = 0;
    /** 初始选中 / Initially selected tab. */
    protected int selected = 0;
    /** 初始滚动像素 / Initial scroll offset in pixels. */
    protected int scrollOffset = 0;
    /** 翻页按钮文字 / Arrow button labels. */
    protected Component previousText = Component.literal("<");
    /** 参见对应设置方法 / See the matching setter. */
    protected Component nextText = Component.literal(">");
    /** 切换回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected TabStripBuilder(int x, int y, int width, List<TabStripItem> tabs) {
        super(x, y, width);
        this.tabs = tabs;
    }

    @Override
    protected final TabStripBuilder self() {
        return this;
    }

    /** 固定在左侧不滚动的标签数 / Leading tabs pinned outside the scroll area. */
    public final TabStripBuilder pinnedLeading(int count) {
        this.pinnedLeading = count;
        return this;
    }

    /** 初始选中 / Initially selected tab. */
    public final TabStripBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动像素 / Initial scroll offset in pixels. */
    public final TabStripBuilder scrollOffset(int pixels) {
        this.scrollOffset = pixels;
        return this;
    }

    /** 翻页按钮文字 / Arrow button labels. */
    public final TabStripBuilder arrows(Component previous, Component next) {
        this.previousText = previous == null ? Component.literal("<") : previous;
        this.nextText = next == null ? Component.literal(">") : next;
        return this;
    }

    /** 切换回调 / Selection callback. */
    public final TabStripBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }
}
