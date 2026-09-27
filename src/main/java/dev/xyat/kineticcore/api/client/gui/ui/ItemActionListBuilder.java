package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.*;
import dev.xyat.kineticcore.api.client.gui.widget.list.*;
import dev.xyat.kineticcore.api.client.search.KineticSuggestion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** 带行尾按钮的物品列表 / Item action list. */
public abstract class ItemActionListBuilder extends LayeredControlBuilder<ItemActionListBuilder, KineticItemActionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<ItemActionItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 行尾按钮宽度 / Trailing action width. */
    protected int actionWidth = 40;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;
    /** 行尾按钮回调 / Action callback. */
    protected Consumer<Integer> onAction = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ItemActionListBuilder(int x, int y, int width, int height, List<ItemActionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final ItemActionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final ItemActionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ItemActionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 行尾按钮宽度 / Trailing action width. */
    public final ItemActionListBuilder actionWidth(int width) {
        this.actionWidth = width;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final ItemActionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 行尾按钮回调 / Action callback. */
    public final ItemActionListBuilder onAction(Consumer<Integer> onAction) {
        this.onAction = onAction;
        return this;
    }
}
