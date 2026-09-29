package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridDensity;
import dev.xyat.kineticcore.api.client.gui.widget.list.ItemGridItem;
import dev.xyat.kineticcore.api.client.gui.widget.list.KineticItemGrid;

import java.util.List;
import java.util.function.Consumer;

/** 物品网格 / Item grid. */
public abstract class ItemGridBuilder extends LayeredControlBuilder<ItemGridBuilder, KineticItemGrid> {
    /** 高度 / Height. */
    protected final int height;
    /** 密度 / Density. */
    protected final ItemGridDensity density;
    /** 物品 / Items. */
    protected final List<ItemGridItem> items;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 点击物品回调 / Item click callback. */
    protected Consumer<Integer> onClick = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ItemGridBuilder(int x, int y, int width, int height, ItemGridDensity density, List<ItemGridItem> items) {
        super(x, y, width);
        this.height = height;
        this.density = density;
        this.items = items;
    }

    @Override
    protected final ItemGridBuilder self() {
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ItemGridBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 点击物品回调 / Item click callback. */
    public final ItemGridBuilder onClick(Consumer<Integer> onClick) {
        this.onClick = onClick;
        return this;
    }
}
