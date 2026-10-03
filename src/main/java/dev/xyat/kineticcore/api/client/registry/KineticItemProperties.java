package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticItemPropertyRuntime;
//? if >=26.1 {
/*import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
*///?} else {
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.item.Item;
import java.util.function.Supplier;
//?}
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Public Kinetic API facade for item properties. */
public final class KineticItemProperties {
    private KineticItemProperties() {
    }

    //? if >=26.1 {
    /*/^*
     * 在客户端初始化前登记数值型物品模型属性，物品模型 JSON 以 {@code id} 引用它（{@code range_dispatch}）。
     * 初始化事件开始后禁止新增登记。
     *
     * <p>Registers a numeric item model property before client setup; item model JSON refers to it by {@code id} in a
     * {@code range_dispatch} model. Since 26.1 item model properties belong to model JSON, not to items.
     *
     * @throws IllegalStateException if client setup has already started
     ^/
    public static void registerRange(ResourceLocation id, MapCodec<? extends RangeSelectItemModelProperty> property) {
        KineticItemPropertyRuntime.registerRange(Objects.requireNonNull(id, "id"), Objects.requireNonNull(property, "property"));
    }

    /^*
     * 在客户端初始化前登记布尔型物品模型属性，物品模型 JSON 以 {@code id} 引用它（{@code condition}）。
     * 初始化事件开始后禁止新增登记。
     *
     * <p>Registers a boolean item model property before client setup; item model JSON refers to it by {@code id} in a
     * {@code condition} model.
     *
     * @throws IllegalStateException if client setup has already started
     ^/
    public static void registerConditional(ResourceLocation id, MapCodec<? extends ConditionalItemModelProperty> property) {
        KineticItemPropertyRuntime.registerConditional(Objects.requireNonNull(id, "id"), Objects.requireNonNull(property, "property"));
    }
    *///?} else {
    /**
     * 在客户端初始化前登记物品模型属性。各属性独立执行，一个供应器失败不会跳过其他属性。 初始化事件开始后禁止新增登记。
     *
     * <p>Registers an item model property before client setup. Each property is applied independently.
     *
     * @throws IllegalStateException if client setup has already started
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
    //?}
}
