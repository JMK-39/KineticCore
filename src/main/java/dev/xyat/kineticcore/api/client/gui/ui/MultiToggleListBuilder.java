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

/** 多开关列表 / Multi-toggle list. */
public abstract class MultiToggleListBuilder extends LayeredControlBuilder<MultiToggleListBuilder, KineticMultiToggleList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<MultiToggleItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;
    /** 开关回调 / Toggle callback. */
    protected BiConsumer<ToggleHit, Boolean> onToggle = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected MultiToggleListBuilder(int x, int y, int width, int height, List<MultiToggleItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final MultiToggleListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final MultiToggleListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final MultiToggleListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final MultiToggleListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 开关回调 / Toggle callback. */
    public final MultiToggleListBuilder onToggle(BiConsumer<ToggleHit, Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }
}
