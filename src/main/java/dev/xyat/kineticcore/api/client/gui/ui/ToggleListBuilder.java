package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.list.KineticToggleList;
import dev.xyat.kineticcore.api.client.gui.widget.list.ToggleItem;

import java.util.List;
import java.util.function.BiConsumer;

/** 开关列表 / Toggle list. */
public abstract class ToggleListBuilder extends LayeredControlBuilder<ToggleListBuilder, KineticToggleList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<ToggleItem> items;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 开关回调 / Toggle callback. */
    protected BiConsumer<Integer, Boolean> onToggle = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ToggleListBuilder(int x, int y, int width, int height, List<ToggleItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final ToggleListBuilder self() {
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ToggleListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 开关回调 / Toggle callback. */
    public final ToggleListBuilder onToggle(BiConsumer<Integer, Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }
}
