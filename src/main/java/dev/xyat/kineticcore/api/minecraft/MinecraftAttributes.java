package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Helpers for vanilla attribute definitions. */
public final class MinecraftAttributes {
    private MinecraftAttributes() {
    }

    /**
     * Writable range of a {@code RangedAttribute}. Implemented by a KineticCore mixin; add-ons call the static
     * helpers instead.
     */
    public interface RangeAccess {
        /** Replaces the attribute's minimum value. */
        void kineticcore$setMinValue(double minValue);

        /** Replaces the attribute's maximum value. */
        void kineticcore$setMaxValue(double maxValue);
    }

    /**
     * Changes the allowed range of an attribute, for example to lift vanilla's armor cap. Affects every entity;
     * call it during setup, before values are clamped.
     *
     * @param attribute attribute to change
     * @param minimum new minimum value
     * @param maximum new maximum value
     */
    public static void setRange(RangedAttribute attribute, double minimum, double maximum) {
        RangeAccess access = (RangeAccess) attribute;
        access.kineticcore$setMinValue(minimum);
        access.kineticcore$setMaxValue(maximum);
    }
}
