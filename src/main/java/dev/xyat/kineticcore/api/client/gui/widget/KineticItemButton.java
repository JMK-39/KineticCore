package dev.xyat.kineticcore.api.client.gui.widget;

import net.minecraft.world.item.ItemStack;

/** 带物品图标的卡片按钮 / Card button showing an item icon. */
public interface KineticItemButton extends KineticButton {
    /** 图标物品 / The icon item. */
    ItemStack icon();
}
