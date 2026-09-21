package dev.xyat.kineticcore.feature.bee.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class BeeMixins {

    // 1. Bee 修复
    @Mixin(Bee.class)
    public static abstract class BeeMixin {
        @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
        private void onReadAdditionalSaveData(CompoundTag compound, CallbackInfo ci) {
            if (compound.contains("NoGravity")) {
                ((Bee) (Object) this).setNoGravity(true);
            }
        }

        @Inject(method = "finalizeSpawn", at = @At("RETURN"))
        private void onFinalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData spawnData, CompoundTag dataTag, CallbackInfoReturnable<SpawnGroupData> cir) {
            ((Bee) (Object) this).setNoGravity(true);
        }

        @Inject(method = "getBreedOffspring", at = @At("RETURN"))
        private void onGetBreedOffspring(ServerLevel level, AgeableMob otherParent, CallbackInfoReturnable<AgeableMob> cir) {
            if (cir.getReturnValue() != null) {
                cir.getReturnValue().setNoGravity(true);
            }
        }
    }

    // 2. Level 修复
    @Mixin(Level.class)
    public static abstract class LevelMixin {
        @Shadow public abstract DimensionType dimensionType();

        @Inject(method = "prepareWeather", at = @At("HEAD"), cancellable = true)
        private void onPrepareWeather(CallbackInfo ci) {
            if (!this.dimensionType().hasSkyLight()) {
                ci.cancel();
            }
        }
    }

    // 3. TurtleEggBlock 修复
    @Mixin(TurtleEggBlock.class)
    public static abstract class TurtleEggBlockMixin {
        @Inject(method = "canDestroyEgg", at = @At("HEAD"), cancellable = true)
        private void preventBeeDestroyingEgg(Level level, Entity entity, CallbackInfoReturnable<Boolean> cir) {
            if (entity instanceof Bee) {
                cir.setReturnValue(false);
            }
        }
    }

    // 4. RandomPos 修复
    @Mixin(RandomPos.class)
    public static abstract class RandomPosMixin {
        @Redirect(method = "generateRandomDirectionWithinRadians", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;containing(DDD)Lnet/minecraft/core/BlockPos;"))
        private static BlockPos onContaining(double x, double y, double z) {
            return BlockPos.containing(x + 0.5D, y, z + 0.5D);
        }
    }

    // 5. PathNavigation 修复 (已解决签名报错)
    @Mixin(PathNavigation.class)
    public static abstract class PathNavigationMixin {
        /**
         * 通过拦截 getBbWidth 返回值实现安全截断。
         * 原版逻辑为: (double)(width + 1.0F) / 2.0D
         * 我们让 getBbWidth 返回 (int)(width + 1.0F) - 1.0F
         * 这样加上 1.0F 后，就正好等于 (int)(width + 1.0F)，完美代替 JS 的 F2I; I2D
         */
        @Redirect(method = "followThePath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getBbWidth()F"))
        private float fixFloatDrift(Mob instance) {
            float width = instance.getBbWidth();
            return (float) ((int) (width + 1.0F)) - 1.0F;
        }
    }
}