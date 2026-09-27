package dev.xyat.kineticcore.api.client.item;

import dev.xyat.kineticcore.api.event.KineticEventSubscription;
import dev.xyat.kineticcore.internal.client.item.HoveredItemRuntime;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * 鼠标下物品的扩展来源。核心只识别原版容器槽位；配方查看器（JEI/EMI/REI）等覆盖层由各自的兼容附属注册提供者。
 * Extension point for "the item under the mouse". The core only knows vanilla container slots; recipe-viewer
 * overlays (JEI/EMI/REI) and similar are contributed by their own optional compat addons.
 */
public final class KineticHoveredItems {
    private KineticHoveredItems() {
    }

    /**
     * 注册提供者；返回空物品表示“不在我这里”。按注册顺序询问，第一个非空结果生效。
     * Registers a provider; return an empty stack for "not mine". Providers are asked in registration order and
     * the first non-empty stack wins.
     */
    public static KineticEventSubscription register(Supplier<ItemStack> provider) {
        return KineticEventSubscription.once(HoveredItemRuntime.register(provider));
    }

    /** 询问已注册的提供者，无结果时返回空物品 / Asks the registered providers; empty when none answers. */
    public static ItemStack resolve() {
        return HoveredItemRuntime.resolve();
    }
}
