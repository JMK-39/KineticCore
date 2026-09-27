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

/** 多按钮列表 / Multi-action list. */
public abstract class MultiActionListBuilder extends LayeredControlBuilder<MultiActionListBuilder, KineticMultiActionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<MultiActionItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;
    /** 行尾按钮回调（行，按钮）/ Action callback (row, action). */
    protected BiConsumer<Integer, Integer> onAction = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected MultiActionListBuilder(int x, int y, int width, int height, List<MultiActionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final MultiActionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final MultiActionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final MultiActionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final MultiActionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 行尾按钮回调（行，按钮）/ Action callback (row, action). */
    public final MultiActionListBuilder onAction(BiConsumer<Integer, Integer> onAction) {
        this.onAction = onAction;
        return this;
    }
}
