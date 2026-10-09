package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Filter after the ordinary pack ordering has been applied. */
@Mixin(PackRepository.class)
public abstract class RecoveryRepositoryMixin {
    @Shadow private List<Pack> selected;
    @Inject(method = {"reload", "setSelected"}, at = @At("RETURN"))
    private void kineticcore$filter(CallbackInfo callback) {
        selected = DatapackRecovery.filter((PackRepository) (Object) this, selected);
    }
}
