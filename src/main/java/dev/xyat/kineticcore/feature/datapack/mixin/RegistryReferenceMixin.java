package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences;
import net.minecraft.core.Holder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegistryFileCodec.class)
public abstract class RegistryReferenceMixin {
    @Shadow @Final private ResourceKey<?> registryKey;
    @Inject(method = "decode", at = @At("HEAD"))
    private <T> void kineticcore$reference(DynamicOps<T> ops, T input, CallbackInfoReturnable<DataResult<Pair<Holder<?>, T>>> callback) {
        ops.getStringValue(input).result().ifPresent(id -> DatapackReferences.value(registryKey, id));
    }
}
