package dev.xyat.kineticcore.api.client.registry;

import dev.xyat.kineticcore.internal.client.registry.KineticItemPropertyRuntime;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticItemProperties {
    private KineticItemProperties() {
    }

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
