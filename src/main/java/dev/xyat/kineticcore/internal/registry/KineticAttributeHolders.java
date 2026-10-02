package dev.xyat.kineticcore.internal.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
//? if >=1.20.5 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
*///?}

/**
 * The public API passes attributes as {@link Attribute} on every Minecraft version. Since 1.20.5 vanilla takes
 * them as registry holders; {@link #of} converts at the call site and is the identity before that.
 */
public final class KineticAttributeHolders {
    private KineticAttributeHolders() {
    }

    //? if >=1.20.5 {
    /*public static Holder<Attribute> of(Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }
    *///?} else {
    public static Attribute of(Attribute attribute) {
        return attribute;
    }
    //?}
}
