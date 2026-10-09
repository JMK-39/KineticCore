package dev.xyat.kineticcore.feature.datapack.mixin;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HolderSetCodec.class)
public abstract class HolderSetReferenceMixin {
    @Shadow @Final private ResourceKey<?> registryKey;
    @Inject(method = "decode", at = @At("HEAD"))
    private <T> void kineticcore$tag(DynamicOps<T> ops, T input, CallbackInfoReturnable<DataResult<Pair<HolderSet<?>, T>>> callback) {
        ops.getStringValue(input).result().filter(value -> value.startsWith("#"))
                .ifPresent(value -> DatapackReferences.tag(registryKey, value.substring(1)));
    }
}
