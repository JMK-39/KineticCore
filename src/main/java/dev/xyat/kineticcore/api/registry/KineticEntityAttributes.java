package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticEntityAttributeRuntime;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Objects;
import java.util.function.Supplier;

public final class KineticEntityAttributes {
    public interface ModificationContext {
        boolean has(EntityType<? extends LivingEntity> entityType, Attribute attribute);

        void add(EntityType<? extends LivingEntity> entityType, Attribute attribute);
    }

    @FunctionalInterface
    public interface ModificationHandler {
        void handle(ModificationContext context);
    }

    private KineticEntityAttributes() {
    }

    public static void onModify(ModificationHandler handler) {
        KineticEntityAttributeRuntime.registerModification(Objects.requireNonNull(handler, "handler"));
    }

    public static <T extends LivingEntity> void registerDefault(
            KineticRegistryHandle<EntityType<T>> entityType,
            Supplier<AttributeSupplier> attributes
    ) {
        KineticEntityAttributeRuntime.registerDefault(
                Objects.requireNonNull(entityType, "entityType"),
                Objects.requireNonNull(attributes, "attributes")
        );
    }
}
