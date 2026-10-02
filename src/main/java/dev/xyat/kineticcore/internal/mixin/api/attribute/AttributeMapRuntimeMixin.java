package dev.xyat.kineticcore.internal.mixin.api.attribute;

import dev.xyat.kineticcore.internal.registry.KineticAttributeHolders;
import dev.xyat.kineticcore.internal.registry.RuntimeAttributeMapAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
//? if >=1.20.5
/*import net.minecraft.core.Holder;*/

import java.util.Map;

@Mixin(AttributeMap.class)
public abstract class AttributeMapRuntimeMixin implements RuntimeAttributeMapAccess {
    //? if >=1.20.5 {
    /*@Shadow @Final private Map<Holder<Attribute>, AttributeInstance> attributes;
    *///?} else {
    @Shadow @Final private Map<Attribute, AttributeInstance> attributes;
    //?}
    @Shadow @Final private AttributeSupplier supplier;

    @Invoker("onAttributeModified")
    public abstract void kineticcore$notifyChanged(AttributeInstance instance);

    @Override
    public AttributeInstance kineticcore$ensure(Attribute attribute) {
        var key = KineticAttributeHolders.of(attribute);
        AttributeMap map = (AttributeMap)(Object)this;
        AttributeInstance existing = map.getInstance(key);
        if (existing != null) return existing;
        AttributeInstance created = new AttributeInstance(key, this::kineticcore$notifyChanged);
        attributes.put(key, created);
        kineticcore$notifyChanged(created);
        return created;
    }

    @Override
    public boolean kineticcore$remove(Attribute attribute) {
        var key = KineticAttributeHolders.of(attribute);
        // A supplier-provided attribute belongs to vanilla/the entity's own mod: never remove it.
        if (supplier.hasAttribute(key)) return false;
        AttributeInstance removed = attributes.remove(key);
        if (removed == null) return false;
        AttributeMap map = (AttributeMap)(Object)this;
        //? if >=1.20.5 {
        /*map.getAttributesToSync().remove(removed);
        map.getAttributesToUpdate().remove(removed);
        *///?} else {
        map.getDirtyAttributes().remove(removed);
        //?}
        return true;
    }
}
