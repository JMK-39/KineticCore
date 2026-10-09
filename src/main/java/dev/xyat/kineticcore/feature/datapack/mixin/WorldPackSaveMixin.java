package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PrimaryLevelData.class)
public abstract class WorldPackSaveMixin {
    //? if >=26.1 {
    /*@ModifyArg(method = "setTagData", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;store(Lcom/mojang/serialization/MapCodec;Ljava/lang/Object;)V"), index = 1)
    *///?} else {
    @ModifyArg(method = "setTagData", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", remap = false), index = 1)
    //?}
    private Object kineticcore$preservePackChoices(Object value) {
        return DatapackRecovery.savedConfiguration((PrimaryLevelData) (Object) this, value);
    }
}
