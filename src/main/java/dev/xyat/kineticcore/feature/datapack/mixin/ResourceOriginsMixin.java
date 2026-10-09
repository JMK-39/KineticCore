package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackReferences;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(MultiPackResourceManager.class)
public abstract class ResourceOriginsMixin {
    @Unique private boolean kineticcore$serverData;
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PackResources> kineticcore$skipResources(List<PackResources> packs, PackType type, List<PackResources> original) {
        return DatapackRecovery.filterResources(type, packs);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    private void kineticcore$type(PackType type, List<PackResources> packs, CallbackInfo callback) {
        kineticcore$serverData = type == PackType.SERVER_DATA;
    }
    @Inject(method = "listResources", at = @At("RETURN"))
    private void kineticcore$origins(CallbackInfoReturnable<Map<ResourceLocation, Resource>> callback) {
        if (kineticcore$serverData) DatapackReferences.remember(callback.getReturnValue());
    }
}
