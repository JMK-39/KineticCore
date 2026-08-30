package dev.xyat.kineticcore.feature.worldmanagement;

import com.sun.jna.platform.FileUtils;
import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.feature.worldmanagement.client.NotificationOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncWorldDeleter {
    private static final AtomicBoolean IS_DELETING = new AtomicBoolean(false);
    private static final Component DELETING_MSG = Component.translatable("msg.kineticcore.deleting_archive");

    public static void moveToTrashOrDelete(Path worldPath) {
        if (!IS_DELETING.getAndSet(true)) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> NotificationOverlay.addNotification(DELETING_MSG, true));
            });
            Thread deleteThread = createDeleteThread(worldPath);
            registerShutdownHook(deleteThread);
            deleteThread.start();
        }
    }

    private static Thread createDeleteThread(Path worldPath) {
        File worldFolder = worldPath.toFile();
        return new Thread(() -> {
            try {
                // 等待文件锁彻底释放
                Thread.sleep(150);

                FileUtils fileUtils = FileUtils.getInstance();
                if (!fileUtils.hasTrash()) {
                    throw new UnsupportedOperationException();
                }

                fileUtils.moveToTrash(worldFolder);

                handleCompletion(true);
            } catch (Exception var6) {
                performPermanentDeletion(worldPath);
            } finally {
                IS_DELETING.set(false);
            }
        }, "World-Deletion-Thread");
    }

    private static void handleCompletion(boolean success) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                NotificationOverlay.removeNotification(DELETING_MSG);
                Screen currentScreen = mc.screen;
                if (currentScreen instanceof SelectWorldScreen sws) {
                    Screen parentScreen = ((ISelectWorldScreen) sws).kineticcore$getLastScreen();
                    mc.setScreen(new SelectWorldScreen(parentScreen));
                }

                NotificationOverlay.addNotification(Component.translatable(
                        success ? "msg.kineticcore.archive_deleted" : "msg.kineticcore.archive_deleted_permanent"
                ));
            });
        });
    }

    private static void registerShutdownHook(Thread deleteThread) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (deleteThread.isAlive()) {
                try {
                    deleteThread.join();
                } catch (InterruptedException ignored) {
                }
            }
        }));
    }

    private static void performPermanentDeletion(Path path) {
        try {
            if (Files.exists(path)) {
                PathUtils.deleteDirectory(path);
                handleCompletion(false);
            }
        } catch (IOException e) {
            KineticCore.LOGGER.error("Failed to delete: {}", path, e);
        }
    }
}