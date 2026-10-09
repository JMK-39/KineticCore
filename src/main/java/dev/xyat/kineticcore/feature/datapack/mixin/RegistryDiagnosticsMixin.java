package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackDiagnostics;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=26.1 {
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.ReportedException;
*///?} else {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import java.util.Map;

@Mixin(RegistryDataLoader.class)
public abstract class RegistryDiagnosticsMixin {
    @Inject(method = "logErrors", at = @At("HEAD"))
    //? if >=26.1 {
    /*private static void kineticcore$diagnose(Map<ResourceKey<?>, Exception> errors, CallbackInfoReturnable<ReportedException> callback) {
    *///?} else {
    private static void kineticcore$diagnose(Map<ResourceKey<?>, Exception> errors, CallbackInfo callback) {
    //?}
        DatapackDiagnostics.recordRegistryErrors(errors);
    }
}
