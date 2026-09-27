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

/** 标准按钮 / Standard button. */
public abstract class ButtonBuilder extends LayeredControlBuilder<ButtonBuilder, KineticButton> {
    /** 文字 / Label. */
    protected Component text = Component.empty();
    /** 点击回调 / Click action. */
    protected Consumer<KineticButton> onClick = null;
    /** 紧凑样式（窄内边距）/ Compact style. */
    protected boolean compact = false;
    /** 内容卡片表面（高度 26，文字隐藏，由页面自绘内容）/ Content-card surface (height 26, label hidden, page draws the content). */
    protected boolean card = false;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ButtonBuilder(int x, int y, int width) {
        super(x, y, width);

    }

    @Override
    protected final ButtonBuilder self() {
        return this;
    }

    /** 文字 / Label. */
    public final ButtonBuilder text(Component text) {
        this.text = text == null ? Component.empty() : text;
        return this;
    }

    /** 点击回调 / Click action. */
    public final ButtonBuilder onClick(Runnable action) {
        this.onClick = action == null ? null : button -> action.run();
        return this;
    }

    /** 带按钮参数的点击回调 / Click handler receiving the button. */
    public final ButtonBuilder onClick(Consumer<KineticButton> handler) {
        this.onClick = handler;
        return this;
    }

    /** 紧凑样式（窄内边距）/ Compact style. */
    public final ButtonBuilder compact() {
        this.compact = true;
        return this;
    }

    /** 内容卡片表面（高度 26，文字隐藏，由页面自绘内容）/ Content-card surface (height 26, label hidden, page draws the content). */
    public final ButtonBuilder card() {
        this.card = true;
        return this;
    }
}
