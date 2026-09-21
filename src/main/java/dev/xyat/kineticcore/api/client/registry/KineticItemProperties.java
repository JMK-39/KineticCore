package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticItemPropertyRuntime;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for item properties. */
public final class KineticItemProperties {
    private KineticItemProperties() {
    }

    /**
     * 在客户端初始化前登记物品模型属性。各属性独立执行，一个供应器失败不会跳过其他属性。
     * 初始化事件开始后禁止新增登记。
     */
    public static void register(
            Supplier<? extends Item> item,
            ResourceLocation id,
            ItemPropertyFunction property
    ) {
        KineticItemPropertyRuntime.register(
                Objects.requireNonNull(item, "item"),
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(property, "property")
        );
    }
}
