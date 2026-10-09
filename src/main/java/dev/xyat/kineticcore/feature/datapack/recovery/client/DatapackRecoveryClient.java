package dev.xyat.kineticcore.feature.datapack.recovery.client;

import dev.xyat.kineticcore.feature.datapack.recovery.DatapackRecovery;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Client-only adapter; common diagnostics never refer to Minecraft client classes. */
public final class DatapackRecoveryClient {
    private static final Logger LOG = LoggerFactory.getLogger(DatapackRecoveryClient.class);
    private DatapackRecoveryClient() { }

    public static boolean replaceFailure(Screen screen) {
        if (!(screen instanceof DatapackLoadFailureScreen) || DatapackRecovery.clientWorld() == null) return false;
        FailureCallbacks callbacks = (FailureCallbacks) screen;
        KineticGui.open(new DatapackRecoveryPage(callbacks.kineticcore$retry(), callbacks.kineticcore$cancel()));
        return true;
    }

    /** Locate a real file, or its archive when the resource is inside a ZIP. */
    static void locate(Path path) {
        if (!Files.exists(path)) return;
        try {
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
                new ProcessBuilder("explorer.exe", "/select,", path.toAbsolutePath().toString()).start();
            } else {
                openDirectory(path.getParent());
            }
        } catch (IOException exception) {
            LOG.error("Cannot locate data pack file {}", path, exception);
            openDirectory(path.getParent());
        }
    }

    static void openDirectory(Path directory) {
        if (directory != null && Files.isDirectory(directory)) Util.getPlatform().openFile(directory.toFile());
    }
}
