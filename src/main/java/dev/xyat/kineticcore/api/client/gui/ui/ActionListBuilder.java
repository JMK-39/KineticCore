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

/** 带行尾按钮的列表 / Action list. */
public abstract class ActionListBuilder extends LayeredControlBuilder<ActionListBuilder, KineticActionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<ActionItem> items;
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
    protected ActionListBuilder(int x, int y, int width, int height, List<ActionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final ActionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final ActionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ActionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 行尾按钮宽度 / Trailing action width. */
    public final ActionListBuilder actionWidth(int width) {
        this.actionWidth = width;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final ActionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 行尾按钮回调 / Action callback. */
    public final ActionListBuilder onAction(Consumer<Integer> onAction) {
        this.onAction = onAction;
        return this;
    }
}
