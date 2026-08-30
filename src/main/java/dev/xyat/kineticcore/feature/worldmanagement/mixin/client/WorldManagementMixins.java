package dev.xyat.kineticcore.feature.worldmanagement.mixin.client;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.mechanics.config.GeneralMechanicsConfig;
import dev.xyat.kineticcore.feature.worldmanagement.AsyncWorldDeleter;
import dev.xyat.kineticcore.feature.worldmanagement.ISelectWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldManagementMixins {

    @Mixin(LevelStorageSource.LevelStorageAccess.class)
    public static abstract class RecycleBin {
        @Inject(method = {"deleteLevel", "m_78311_"}, at = @At("HEAD"), cancellable = true, remap = false)
        private void kineticcore$deleteToRecycleBin(CallbackInfo ci) {
            if (!GeneralMechanicsConfig.recycleBinWorlds) {
                return;
            }

            Path worldPath = ((LevelStorageSource.LevelStorageAccess) (Object) this).getLevelPath(LevelResource.ROOT);
            if (!Files.exists(worldPath)) {
                return;
            }

            try {
                ((AutoCloseable) this).close();
                AsyncWorldDeleter.moveToTrash(worldPath);
            } catch (Exception e) {
                KineticCore.LOGGER.error("kineticcore: Error preparing world for recycle-bin deletion", e);
            }

            ci.cancel();
        }
    }

    @Mixin(SelectWorldScreen.class)
    public static abstract class Navigation implements ISelectWorldScreen {
        @Unique
        private Screen kineticcore$capturedLastScreen;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void kineticcore$captureLastScreen(Screen lastScreen, CallbackInfo ci) {
            this.kineticcore$capturedLastScreen = lastScreen;
        }

        @Override
        public Screen kineticcore$getLastScreen() {
            return this.kineticcore$capturedLastScreen;
        }
    }
}
