package dev.xyat.kineticcore.internal.client.registry;

//? if >=26.1 {
/*import com.mojang.serialization.MapCodec;
import dev.xyat.kineticcore.internal.runtime.KineticModContextRuntime;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
*///?} else {
import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;
//?}

public final class KineticItemPropertyRuntime {
    private KineticItemPropertyRuntime() {
    }

    //? if >=26.1 {
    /*// One listener per property: a broken codec must not hide others.
    public static synchronized void registerRange(ResourceLocation id, MapCodec<? extends RangeSelectItemModelProperty> property) {
        KineticModContextRuntime.modEventBus().addListener((RegisterRangeSelectItemModelPropertyEvent event) -> event.register(id, property));
    }

    public static synchronized void registerConditional(ResourceLocation id, MapCodec<? extends ConditionalItemModelProperty> property) {
        KineticModContextRuntime.modEventBus().addListener((RegisterConditionalItemModelPropertyEvent event) -> event.register(id, property));
    }
    *///?} else {
    public static synchronized void register(
            Supplier<? extends Item> item,
            ResourceLocation id,
            ItemPropertyFunction property
    ) {
        // Submit each property independently so a broken supplier cannot skip others.
        // The lifecycle owns the late-registration guard and setup scheduling.
        Registration registration = new Registration(item, id, property);
        KineticModLifecycleRuntime.onClientSetup(() ->
                ItemProperties.register(registration.item().get(), registration.id(), registration.property()));
    }

    private record Registration(
            Supplier<? extends Item> item,
            ResourceLocation id,
            ItemPropertyFunction property
    ) {
    }
    //?}
}
