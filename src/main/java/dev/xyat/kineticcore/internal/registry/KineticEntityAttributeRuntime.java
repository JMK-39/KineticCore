package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.api.registry.KineticEntityAttributes;
import dev.xyat.kineticcore.api.registry.KineticRegistryHandle;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class KineticEntityAttributeRuntime {
    private static final List<Registration<?>> DEFAULTS = new ArrayList<>();
    private static final List<KineticEntityAttributes.ModificationHandler> MODIFICATIONS = new ArrayList<>();
    private static boolean initialized;

    private KineticEntityAttributeRuntime() {
    }

    public static synchronized void registerModification(KineticEntityAttributes.ModificationHandler handler) {
        initialize();
        MODIFICATIONS.add(handler);
    }

    public static synchronized <T extends LivingEntity> void registerDefault(
            KineticRegistryHandle<EntityType<T>> entityType,
            Supplier<AttributeSupplier> attributes
    ) {
        initialize();
        DEFAULTS.add(new Registration<>(entityType, attributes));
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticEntityAttributeRuntime::onCreateAttributes);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(KineticEntityAttributeRuntime::onModifyAttributes);
    }

    private static void onModifyAttributes(EntityAttributeModificationEvent event) {
        ModificationContextImpl context = new ModificationContextImpl(event);
        for (KineticEntityAttributes.ModificationHandler handler : List.copyOf(MODIFICATIONS)) {
            handler.handle(context);
        }
    }

    private static void onCreateAttributes(EntityAttributeCreationEvent event) {
        for (Registration<?> registration : List.copyOf(DEFAULTS)) {
            registration.apply(event);
        }
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
