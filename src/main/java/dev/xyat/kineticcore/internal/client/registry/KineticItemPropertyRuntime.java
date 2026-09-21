package dev.xyat.kineticcore.internal.client.registry;

import dev.xyat.kineticcore.internal.runtime.KineticModLifecycleRuntime;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class KineticItemPropertyRuntime {
    private KineticItemPropertyRuntime() {
    }

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
}
