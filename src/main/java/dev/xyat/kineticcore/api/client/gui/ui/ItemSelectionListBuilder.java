package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemSelectionItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemSelectionList;

import java.util.List;
import java.util.function.Consumer;

/** 物品单选列表 / Item selection list. */
public abstract class ItemSelectionListBuilder extends LayeredControlBuilder<ItemSelectionListBuilder, KineticItemSelectionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<ItemSelectionItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ItemSelectionListBuilder(int x, int y, int width, int height, List<ItemSelectionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final ItemSelectionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final ItemSelectionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ItemSelectionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final ItemSelectionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }
}
