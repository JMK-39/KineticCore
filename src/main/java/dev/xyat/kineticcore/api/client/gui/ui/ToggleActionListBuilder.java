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

/** 带开关与按钮的列表 / Toggle-action list. */
public abstract class ToggleActionListBuilder extends LayeredControlBuilder<ToggleActionListBuilder, KineticToggleActionList> {
    /** 高度 / Height. */
    protected final int height;
    /** 行数据 / Rows. */
    protected final List<ToggleActionItem> items;
    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    protected int selected = -1;
    /** 初始滚动行 / Initial scroll offset in rows. */
    protected int scrollOffset = 0;
    /** 开关宽度 / Toggle width. */
    protected int toggleWidth = 40;
    /** 行尾按钮宽度 / Action width. */
    protected int actionWidth = 40;
    /** 选中回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;
    /** 开关回调 / Toggle callback. */
    protected BiConsumer<Integer, Boolean> onToggle = null;
    /** 行尾按钮回调 / Action callback. */
    protected Consumer<Integer> onAction = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ToggleActionListBuilder(int x, int y, int width, int height, List<ToggleActionItem> items) {
        super(x, y, width);
        this.height = height;
        this.items = items;
    }

    @Override
    protected final ToggleActionListBuilder self() {
        return this;
    }

    /** 初始选中行，-1 无 / Initially selected row; -1 for none. */
    public final ToggleActionListBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 初始滚动行 / Initial scroll offset in rows. */
    public final ToggleActionListBuilder scrollOffset(int rows) {
        this.scrollOffset = rows;
        return this;
    }

    /** 开关宽度 / Toggle width. */
    public final ToggleActionListBuilder toggleWidth(int width) {
        this.toggleWidth = width;
        return this;
    }

    /** 行尾按钮宽度 / Action width. */
    public final ToggleActionListBuilder actionWidth(int width) {
        this.actionWidth = width;
        return this;
    }

    /** 选中回调 / Selection callback. */
    public final ToggleActionListBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 开关回调 / Toggle callback. */
    public final ToggleActionListBuilder onToggle(BiConsumer<Integer, Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    /** 行尾按钮回调 / Action callback. */
    public final ToggleActionListBuilder onAction(Consumer<Integer> onAction) {
        this.onAction = onAction;
        return this;
    }
}
