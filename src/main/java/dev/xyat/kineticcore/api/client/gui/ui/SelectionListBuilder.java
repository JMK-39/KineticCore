package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticSelectionList;
import dev.xyat.kineticcore.api.client.gui.widget.list.SelectionItem;

import java.util.List;
import java.util.function.Consumer;

/** 单选列表 / Selection list. */
public abstract class SelectionListBuilder extends LayeredControlBuilder<SelectionListBuilder, KineticSelectionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<SelectionItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 纯文字行（无按钮边框），用于 ID 列表 / Plain text rows without button frames, for identifier lists. */
    protected boolean textRows = false;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected SelectionListBuilder(int x, int y, int width, int height, List<SelectionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final SelectionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final SelectionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final SelectionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final SelectionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 纯文字行（无按钮边框），用于群系、伤害类型、属性等 ID 列表 / Draws plain text rows without button frames, for identifier lists such as biomes, damage types or attributes. */
    public final SelectionListBuilder textRows() {
        this.textRows = true;
        return this;
    }
}
