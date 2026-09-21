package dev.xyat.kineticcore.feature.damageindicator.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(
            method = {
                    "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
                    "m_8767_(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
            },
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private <T extends ParticleOptions> void disableDamageParticles(
            T pType, double pPosX, double pPosY, double pPosZ, int pParticleCount,
            double pXOffset, double pYOffset, double pZOffset, double pSpeed,
            CallbackInfoReturnable<Integer> cir
    ) {
        // 如果是原版的伤害指示器粒子，直接强制返回 0，彻底掐断！
        if (pType == ParticleTypes.DAMAGE_INDICATOR) {
            cir.setReturnValue(0);
        }
    }
}