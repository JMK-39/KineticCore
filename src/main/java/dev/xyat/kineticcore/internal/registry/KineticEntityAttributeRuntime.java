package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class KineticEntityAttributeRuntime {
    private static final Map<ResourceLocation, Registration<?>> DEFAULTS = new LinkedHashMap<>();
    private static final List<KineticEntityAttributes.ModificationHandler> MODIFICATIONS = new ArrayList<>();
    private static boolean creationListenerRegistered;
    private static boolean modificationListenerRegistered;
    private static boolean creationRegistrationClosed;
    private static boolean modificationRegistrationClosed;

    private KineticEntityAttributeRuntime() {
    }

    public static synchronized void registerModification(KineticEntityAttributes.ModificationHandler handler) {
        Objects.requireNonNull(handler, "handler");
        if (modificationRegistrationClosed) {
            throw new IllegalStateException("Entity attribute modification registration window has already closed");
        }
        initialize();
        MODIFICATIONS.add(handler);
    }

    public static synchronized <T extends LivingEntity> void registerDefault(
            KineticRegistryHandle<EntityType<T>> entityType,
            Supplier<AttributeSupplier> attributes
    ) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(attributes, "attributes");
        if (creationRegistrationClosed) {
            throw new IllegalStateException("Default attribute registration window has already closed");
        }
        ResourceLocation id = Objects.requireNonNull(entityType.id(), "entityType.id()");
        if (DEFAULTS.containsKey(id)) {
            throw new IllegalStateException("Default attributes already registered for entity type: " + id);
        }
        initialize();
        DEFAULTS.put(id, new Registration<>(entityType, attributes));
    }

    private static void initialize() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (!creationListenerRegistered) {
            modEventBus.addListener(KineticEntityAttributeRuntime::onCreateAttributes);
            creationListenerRegistered = true;
        }
        if (!modificationListenerRegistered) {
            modEventBus.addListener(KineticEntityAttributeRuntime::onModifyAttributes);
            modificationListenerRegistered = true;
        }
    }

    private static void onModifyAttributes(EntityAttributeModificationEvent event) {
        List<KineticEntityAttributes.ModificationHandler> handlers;
        synchronized (KineticEntityAttributeRuntime.class) {
            modificationRegistrationClosed = true;
            handlers = List.copyOf(MODIFICATIONS);
        }
        ModificationContextImpl context = new ModificationContextImpl(event);
        RuntimeException failure = null;
        for (KineticEntityAttributes.ModificationHandler handler : handlers) {
            try {
                handler.handle(context);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private static void onCreateAttributes(EntityAttributeCreationEvent event) {
        List<Registration<?>> registrations;
        synchronized (KineticEntityAttributeRuntime.class) {
            creationRegistrationClosed = true;
            registrations = List.copyOf(DEFAULTS.values());
        }
        RuntimeException failure = null;
        for (Registration<?> registration : registrations) {
            try {
                registration.apply(event);
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else if (failure != exception) failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }

    private record ModificationContextImpl(EntityAttributeModificationEvent event) implements KineticEntityAttributes.ModificationContext {
        @Override
        public boolean has(EntityType<? extends LivingEntity> entityType, Attribute attribute) {
            return event.has(entityType, attribute);
        }

        @Override
        public void add(EntityType<? extends LivingEntity> entityType, Attribute attribute) {
            event.add(entityType, attribute);
        }
    }

    private record Registration<T extends LivingEntity>(
            KineticRegistryHandle<EntityType<T>> entityType,
            Supplier<AttributeSupplier> attributes
    ) {
        void apply(EntityAttributeCreationEvent event) {
            event.put(entityType.get(), attributes.get());
        }
    }
}
