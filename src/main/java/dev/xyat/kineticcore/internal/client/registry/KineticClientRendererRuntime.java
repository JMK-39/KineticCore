package dev.xyat.kineticcore.internal.client.registry;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class KineticClientRendererRuntime {
    private static final List<EntityRendererRegistration<?>> ENTITY_RENDERERS = new ArrayList<>();
    private static boolean listenerRegistered;

    private KineticClientRendererRuntime() {
    }

    public static synchronized <T extends Entity> void registerEntityRenderer(
            Supplier<? extends EntityType<T>> entityType,
            EntityRendererProvider<T> provider
    ) {
        ENTITY_RENDERERS.add(new EntityRendererRegistration<>(entityType, provider));
        ensureListener();
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticClientRendererRuntime::onRegisterRenderers);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        List<EntityRendererRegistration<?>> registrations;
        synchronized (KineticClientRendererRuntime.class) {
            registrations = List.copyOf(ENTITY_RENDERERS);
        }
        for (EntityRendererRegistration<?> registration : registrations) {
            registration.register(event);
        }
    }

    private record EntityRendererRegistration<T extends Entity>(
            Supplier<? extends EntityType<T>> entityType,
            EntityRendererProvider<T> provider
    ) {
        private void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(entityType.get(), provider);
        }
    }
}
