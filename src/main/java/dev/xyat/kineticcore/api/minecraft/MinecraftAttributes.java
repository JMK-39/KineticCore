package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Public API type for minecraft attributes. */
public final class MinecraftAttributes {
    private MinecraftAttributes() {
    }

    /** Access contract for range operations. */
    public interface RangeAccess {
        void kineticcore$setMinValue(double minValue);

        void kineticcore$setMaxValue(double maxValue);
    }

    /**
     * Updates range.
     */
    public static void setRange(RangedAttribute attribute, double minimum, double maximum) {
        RangeAccess access = (RangeAccess) attribute;
        access.kineticcore$setMinValue(minimum);
        access.kineticcore$setMaxValue(maximum);
    }
}
