package dev.xyat.kineticcore.internal.client.registry;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class KineticItemPropertyRuntime {
    private static final List<Registration> REGISTRATIONS = new ArrayList<>();
    private static boolean listenerRegistered;

    private KineticItemPropertyRuntime() {
    }

    public static synchronized void register(
            Supplier<? extends Item> item,
            ResourceLocation id,
            ItemPropertyFunction property
    ) {
        REGISTRATIONS.add(new Registration(item, id, property));
        ensureListener();
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticItemPropertyRuntime::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        List<Registration> registrations;
        synchronized (KineticItemPropertyRuntime.class) {
            registrations = List.copyOf(REGISTRATIONS);
        }
        event.enqueueWork(() -> {
            for (Registration registration : registrations) {
                ItemProperties.register(registration.item().get(), registration.id(), registration.property());
            }
        });
    }

    private record Registration(
            Supplier<? extends Item> item,
            ResourceLocation id,
            ItemPropertyFunction property
    ) {
    }
}
