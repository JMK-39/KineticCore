package dev.xyat.kineticcore.internal.mixin.api.attribute;

import dev.xyat.kineticcore.internal.registry.RuntimeAttributeInstanceAccess;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AttributeInstance.class)
public interface AttributeInstanceInvoker extends RuntimeAttributeInstanceAccess {
    @Override
    @Invoker("setDirty")
    void kineticcore$invalidate();
}
