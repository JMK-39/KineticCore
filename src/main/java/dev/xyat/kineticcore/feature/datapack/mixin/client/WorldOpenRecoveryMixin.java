package dev.xyat.kineticcore.feature.datapack.mixin.client;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackDiagnostics;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenRecoveryMixin {
    @Shadow @Final private LevelStorageSource levelSource;

    //? if >=1.21 {
    /*@Inject(method = "openWorld", at = @At("HEAD"))
    private void kineticcore$begin(String name, Runnable back, CallbackInfo callback) {
    *///?} else {
    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void kineticcore$begin(Screen parent, String name, CallbackInfo callback) {
    //?}
        DatapackRecovery.beginWorld(levelSource.getBaseDir().resolve(name));
    }

    //? if >=1.21 {
    /*@ModifyVariable(method = "openWorldLoadLevelStem", at = @At("HEAD"), argsOnly = true)
    *///?} else {
    @ModifyVariable(method = "doLoadLevel", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    //?}
    private boolean kineticcore$normalRetry(boolean safeMode) { return DatapackRecovery.normalRetry(safeMode); }

    //? if >=1.21 {
    /*@Redirect(method = "openWorldLoadLevelStem", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V", remap = false))
    *///?} else {
    @Redirect(method = "doLoadLevel", remap = false, at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V", remap = false))
    //?}
    private void kineticcore$recordFailure(Logger logger, String message, Throwable error) {
        DatapackDiagnostics.recordWorldFailure(error);
        logger.warn(message, error);
    }

    //? if >=1.21 {
    /*@Inject(method = "loadWorldStem", at = @At("RETURN"))
    *///?} else {
    @Inject(method = "loadWorldStem(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;ZLnet/minecraft/server/packs/repository/PackRepository;)Lnet/minecraft/server/WorldStem;", at = @At("RETURN"))
    //?}
    private void kineticcore$preserveChoices(CallbackInfoReturnable<WorldStem> callback) {
        //? if >=26.1 {
        /*var data = callback.getReturnValue().worldDataAndGenSettings().data();
        *///?} else {
        var data = callback.getReturnValue().worldData();
        //?}
        if (data instanceof PrimaryLevelData primary) DatapackRecovery.rememberWorldData(primary);
    }
}
