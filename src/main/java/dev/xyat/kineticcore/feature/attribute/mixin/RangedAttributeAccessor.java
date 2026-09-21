package dev.xyat.kineticcore.feature.attribute.mixin;

import dev.xyat.kineticcore.api.minecraft.MinecraftAttributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor extends MinecraftAttributes.RangeAccess {

    @Accessor("minValue")
    @Mutable
    void kineticcore$setMinValue(double minValue);

    @Accessor("maxValue")
    @Mutable
    void kineticcore$setMaxValue(double maxValue);
}