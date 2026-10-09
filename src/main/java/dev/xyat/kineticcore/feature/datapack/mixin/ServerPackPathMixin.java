package dev.xyat.kineticcore.feature.datapack.mixin;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPacksSource.class)
public abstract class ServerPackPathMixin {
    @Inject(method = "createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;", at = @At("RETURN"))
    private static void kineticcore$rememberWorld(LevelStorageSource.LevelStorageAccess access, CallbackInfoReturnable<PackRepository> callback) {
        DatapackRecovery.bindWorldRepository(callback.getReturnValue(), access.getLevelPath(LevelResource.ROOT));
    }
}
