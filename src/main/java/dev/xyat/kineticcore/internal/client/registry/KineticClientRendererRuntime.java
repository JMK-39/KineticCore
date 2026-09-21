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
    private static boolean registrationClosed;

    private KineticClientRendererRuntime() {
    }

    public static synchronized <T extends Entity> void registerEntityRenderer(
            Supplier<? extends EntityType<T>> entityType,
            EntityRendererProvider<T> provider
    ) {
        if (registrationClosed) {
            throw new IllegalStateException("Entity renderer registration window has already closed");
        }
        // Do not retain an entry when Forge rejects the listener installation.
        ensureListener();
        ENTITY_RENDERERS.add(new EntityRendererRegistration<>(entityType, provider));
    }

    private static void ensureListener() {
        if (listenerRegistered) return;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticClientRendererRuntime::onRegisterRenderers);
        listenerRegistered = true;
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        List<EntityRendererRegistration<?>> registrations;
        synchronized (KineticClientRendererRuntime.class) {
            registrationClosed = true;
            registrations = List.copyOf(ENTITY_RENDERERS);
        }
        RuntimeException failure = null;
        for (EntityRendererRegistration<?> registration : registrations) {
            try {
                registration.register(event);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
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
