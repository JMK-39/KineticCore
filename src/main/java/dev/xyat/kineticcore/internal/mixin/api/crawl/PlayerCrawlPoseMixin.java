package dev.xyat.kineticcore.internal.mixin.api.crawl;

import dev.xyat.kineticcore.internal.runtime.KineticCommonHookRuntime;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerCrawlPoseMixin extends LivingEntity {

    protected PlayerCrawlPoseMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "updatePlayerPose", at = @At("HEAD"), cancellable = true)
    private void kineticcore$injectUpdatePlayerPose(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (KineticCommonHookRuntime.handleCrawlPose(player)) {
            ci.cancel();
        }
    }
}
