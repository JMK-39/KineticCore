package dev.xyat.kineticcore.internal.mixin.api.attribute;

import dev.xyat.kineticcore.internal.registry.RuntimeAttributeMapAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(AttributeMap.class)
public abstract class AttributeMapRuntimeMixin implements RuntimeAttributeMapAccess {
    @Shadow @Final private Map<Attribute, AttributeInstance> attributes;
    @Shadow @Final private AttributeSupplier supplier;

    @Invoker("onAttributeModified")
    public abstract void kineticcore$notifyChanged(AttributeInstance instance);

    @Override
    public AttributeInstance kineticcore$ensure(Attribute attribute) {
        AttributeMap map = (AttributeMap)(Object)this;
        AttributeInstance existing = map.getInstance(attribute);
        if (existing != null) return existing;
        AttributeInstance created = new AttributeInstance(attribute, this::kineticcore$notifyChanged);
        attributes.put(attribute, created);
        kineticcore$notifyChanged(created);
        return created;
    }

    @Override
    public boolean kineticcore$remove(Attribute attribute) {
        // A supplier-provided attribute belongs to vanilla/the entity's own mod: never remove it.
        if (supplier.hasAttribute(attribute)) return false;
        AttributeInstance removed = attributes.remove(attribute);
        if (removed == null) return false;
        ((AttributeMap)(Object)this).getDirtyAttributes().remove(removed);
        return true;
    }
}
