package dev.xyat.kineticcore.internal.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/** Internal bridge: only the public KineticEntityAttributes facade may expose these operations. */
public interface RuntimeAttributeMapAccess {
    AttributeInstance kineticcore$ensure(Attribute attribute);
    boolean kineticcore$remove(Attribute attribute);
}
