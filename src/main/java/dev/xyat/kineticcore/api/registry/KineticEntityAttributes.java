package dev.xyat.kineticcore.api.registry;

import dev.xyat.kineticcore.internal.registry.KineticEntityAttributeRuntime;
import dev.xyat.kineticcore.internal.registry.RuntimeAttributeMapAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Objects;
import java.util.function.Supplier;

/** Public Kinetic API facade for entity attributes. */
public final class KineticEntityAttributes {
    /** Lets an {@link #onModify(ModificationHandler)} callback add attributes to existing entity types. */
    public interface ModificationContext {
        /**
         * Returns whether the entity type already has the attribute, including additions made earlier in this pass.
         */
        boolean has(EntityType<? extends LivingEntity> entityType, Attribute attribute);

        /** Adds the attribute with its default base value to the entity type. */
        void add(EntityType<? extends LivingEntity> entityType, Attribute attribute);
    }

    /** Callback that adds attributes to entity types while Forge builds attribute suppliers. */
    @FunctionalInterface
    public interface ModificationHandler {
        /** Called once during mod loading; use the context to add attributes. */
        void handle(ModificationContext context);
    }

    private KineticEntityAttributes() {
    }

    /** Makes an already registered attribute available on this entity at runtime, on either logical side. */
    public static AttributeInstance ensureInstance(LivingEntity entity, Attribute attribute) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(attribute, "attribute");
        return ((RuntimeAttributeMapAccess) entity.getAttributes()).kineticcore$ensure(attribute);
    }

    /** Removes only attributes absent from the entity's original type supplier. */
    public static boolean removeRuntimeInstance(LivingEntity entity, Attribute attribute) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(attribute, "attribute");
        return ((RuntimeAttributeMapAccess) entity.getAttributes()).kineticcore$remove(attribute);
    }

    /**
     * 注册实体属性修改回调，必须在 Forge 属性修改事件开始前调用。 注册窗口关闭后抛出 IllegalStateException；某一回调失败不会阻断后续回调， 所有回调处理结束后会报告异常。
     *
     * <p>Registers a callback that adds attributes to existing entity types. Call it before Forge's attribute
     * modification event starts.
     *
     * @throws IllegalStateException if the registration window has closed
     */
    public static void onModify(ModificationHandler handler) {
        KineticEntityAttributeRuntime.registerModification(Objects.requireNonNull(handler, "handler"));
    }

    /**
     * 注册实体默认属性，必须在 Forge 属性创建事件开始前调用。 同一实体 ID 不得重复注册；窗口关闭后抛出 IllegalStateException。 单个属性供应器失败不会跳过其他实体属性，但失败仍会报告。
     *
     * <p>Registers the default attributes of a custom entity type. Call it before Forge's attribute creation event
     * starts; each entity type may be registered once.
     *
     * @throws IllegalStateException if the entity type is already registered or the registration window has closed
     */
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
