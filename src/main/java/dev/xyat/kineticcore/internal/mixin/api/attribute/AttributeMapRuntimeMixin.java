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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    // Recreate only registered attributes present in saved data; let vanilla restore their values/modifiers.
    //? if >=26.1 {
    /*@Inject(method = "apply", at = @At("HEAD"))
    private void kineticcore$restoreRuntimeAttributes(java.util.List<AttributeInstance.Packed> packed, CallbackInfo ci) {
        for (var entry : packed) kineticcore$ensure(entry.attribute().value());
    }
    *///?} else {
    @Inject(method = "load", at = @At("HEAD"))
    private void kineticcore$restoreRuntimeAttributes(net.minecraft.nbt.ListTag list, CallbackInfo ci) {
        for (int i = 0; i < list.size(); i++) {
            var tag = list.getCompound(i);
            //? if >=1.21 {
            /*String name = tag.getString("id");
            *///?} else {
            String name = tag.getString("Name");
            //?}
            var id = dev.xyat.kineticcore.api.resource.KineticResourceIds.tryParse(name);
            var registry = dev.xyat.kineticcore.api.registry.KineticRegistries.attributes();
            if (id != null && registry.contains(id)) kineticcore$ensure(registry.get(id));
        }
    }
    //?}

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
