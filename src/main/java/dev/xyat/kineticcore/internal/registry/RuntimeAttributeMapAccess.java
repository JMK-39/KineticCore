package dev.xyat.kineticcore.internal.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/** Internal runtime contract implemented by the AttributeMap Mixin; not part of the public API. */
public interface RuntimeAttributeMapAccess {
    AttributeInstance kineticcore$ensure(Attribute attribute);
    boolean kineticcore$remove(Attribute attribute);
}
