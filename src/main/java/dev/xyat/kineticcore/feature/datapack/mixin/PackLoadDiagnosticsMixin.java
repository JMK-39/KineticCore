package dev.xyat.kineticcore.feature.datapack.mixin;

import com.mojang.datafixers.util.Pair;
import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldLoader.PackConfig.class)
public abstract class PackLoadDiagnosticsMixin {
    @Inject(method = "createResourceManager", at = @At("HEAD"))
    private void kineticcore$beginAttempt(CallbackInfoReturnable<Pair<WorldDataConfiguration, CloseableResourceManager>> callback) {
        DatapackRecovery.beginAttempt(((WorldLoader.PackConfig) (Object) this).packRepository());
    }
}
