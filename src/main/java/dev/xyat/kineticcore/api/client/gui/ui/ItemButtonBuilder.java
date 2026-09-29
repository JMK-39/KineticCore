package dev.xyat.kineticcore.api.client.gui.ui;

import dev.xyat.kineticcore.api.client.gui.widget.KineticItemButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** 物品卡片按钮 / Item card button. */
public abstract class ItemButtonBuilder extends ControlBuilder<ItemButtonBuilder, KineticItemButton> {
    /** 图标 / Icon. */
    protected final ItemStack icon;
    /** 文字 / Label. */
    protected Component text = Component.empty();
    /** 点击回调 / Click action. */
    protected Runnable onClick = null;

    /** 由 KineticUi 创建 / Created by KineticUi. */
    protected ItemButtonBuilder(int x, int y, int width, ItemStack icon) {
        super(x, y, width);
        this.icon = icon;
    }

    @Override
    protected final ItemButtonBuilder self() {
        return this;
    }

    /** 文字 / Label. */
    public final ItemButtonBuilder text(Component text) {
        this.text = text == null ? Component.empty() : text;
        return this;
    }

    /** 点击回调 / Click action. */
    public final ItemButtonBuilder onClick(Runnable action) {
        this.onClick = action;
        return this;
    }
}
