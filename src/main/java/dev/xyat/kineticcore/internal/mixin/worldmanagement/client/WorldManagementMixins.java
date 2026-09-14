package dev.xyat.kineticcore.internal.mixin.worldmanagement.client;

import dev.xyat.kineticcore.api.hook.ServerHooks;
import dev.xyat.kineticcore.api.minecraft.MinecraftScreens;
import dev.xyat.kineticcore.internal.runtime.KineticServerHookRuntime;
import dev.xyat.kineticcore.api.runtime.KineticRuntime;
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
            Path worldPath = ((LevelStorageSource.LevelStorageAccess) (Object) this).getLevelPath(LevelResource.ROOT);
            if (!Files.exists(worldPath)) return;

            ServerHooks.WorldDeletionHandler handler = KineticServerHookRuntime.worldDeletionHandler(worldPath);
            if (handler == null || !handler.shouldRecycle(worldPath)) return;

            try {
                ((AutoCloseable) this).close();
                handler.recycle(worldPath);
            } catch (Exception e) {
                KineticRuntime.logger().error("kineticcore: Error preparing world for recycle-bin deletion", e);
            }

            ci.cancel();
        }
    }

    @Mixin(SelectWorldScreen.class)
    public static abstract class Navigation implements MinecraftScreens.ParentAccess {
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
