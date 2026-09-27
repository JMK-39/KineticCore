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

/** 等宽标签栏 / Tab bar. */
public abstract class TabBarBuilder extends LayeredControlBuilder<TabBarBuilder, KineticTabBar> {
    /** 标签文字 / Tab labels. */
    protected final List<Component> labels;
    /** 每个标签的提示 / Per-tab tooltips. */
    protected List<Component> tooltips = List.of();
    /** 初始选中 / Initially selected tab. */
    protected int selected = 0;
    /** 切换回调 / Selection callback. */
    protected Consumer<Integer> onSelect = null;
    /** 竖排（宽度为单个标签宽度）/ Vertical layout (width is one tab). */
    protected boolean vertical = false;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected TabBarBuilder(int x, int y, int width, List<Component> labels) {
        super(x, y, width);
        this.labels = labels;
    }

    @Override
    protected final TabBarBuilder self() {
        return this;
    }

    /** 每个标签的提示 / Per-tab tooltips. */
    public final TabBarBuilder tooltips(List<Component> tooltips) {
        this.tooltips = tooltips == null ? List.of() : tooltips;
        return this;
    }

    /** 初始选中 / Initially selected tab. */
    public final TabBarBuilder selected(int index) {
        this.selected = index;
        return this;
    }

    /** 切换回调 / Selection callback. */
    public final TabBarBuilder onSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /** 竖排（宽度为单个标签宽度）/ Vertical layout (width is one tab). */
    public final TabBarBuilder vertical() {
        this.vertical = true;
        return this;
    }
}
