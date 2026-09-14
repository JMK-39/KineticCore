package dev.xyat.kineticcore.api.minecraft;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class MinecraftAttributes {
    private MinecraftAttributes() {
    }

    public interface RangeAccess {
        void kineticcore$setMinValue(double minValue);

        void kineticcore$setMaxValue(double maxValue);
    }

    public static void setRange(RangedAttribute attribute, double minimum, double maximum) {
        RangeAccess access = (RangeAccess) attribute;
        access.kineticcore$setMinValue(minimum);
        access.kineticcore$setMaxValue(maximum);
    }
}
