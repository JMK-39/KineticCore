package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class KineticPlatformRuntime {
    private KineticPlatformRuntime() {
    }

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static String displayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    public static List<KineticPlatform.LoadedMod> loadedMods() {
        List<KineticPlatform.LoadedMod> result = new ArrayList<>();
        for (var mod : ModList.get().getMods()) {
            Path sourcePath = null;
            try {
                if (mod.getOwningFile() != null && mod.getOwningFile().getFile() != null) {
                    sourcePath = mod.getOwningFile().getFile().getFilePath();
                }
            } catch (RuntimeException ignored) {
            }
            result.add(new KineticPlatform.LoadedMod(mod.getModId(), sourcePath));
        }
        return List.copyOf(result);
    }
}
