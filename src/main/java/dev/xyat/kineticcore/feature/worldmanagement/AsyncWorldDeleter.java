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

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncWorldDeleter {
    private static final AtomicBoolean IS_DELETING = new AtomicBoolean(false);
    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean(false);
    private static final AtomicReference<Thread> ACTIVE_DELETE_THREAD = new AtomicReference<>();
    private static final Component DELETING_MSG = Component.translatable("msg.kineticcore.deleting_archive");

    public static boolean moveToTrash(Path worldPath) {
        if (!IS_DELETING.compareAndSet(false, true)) {
            showDeletingNotification();
            return false;
        }

        showDeletingNotification();
        registerShutdownHookOnce();

        Thread deleteThread = createDeleteThread(worldPath);
        ACTIVE_DELETE_THREAD.set(deleteThread);
        deleteThread.start();
        return true;
    }

    private static void showDeletingNotification() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> NotificationOverlay.addNotification(DELETING_MSG, true));
        });
    }

    private static Thread createDeleteThread(Path worldPath) {
        File worldFolder = worldPath.toFile();
        return new Thread(() -> {
            try {
                Thread.sleep(150L);

                FileUtils fileUtils = FileUtils.getInstance();
                if (!fileUtils.hasTrash()) {
                    throw new IllegalStateException("System recycle bin is unavailable");
                }

                fileUtils.moveToTrash(worldFolder);
                handleCompletion(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                KineticCore.LOGGER.error("World recycle operation was interrupted: {}", worldPath, e);
                handleCompletion(false);
            } catch (Exception e) {
                KineticCore.LOGGER.error("Failed to move world to system recycle bin: {}", worldPath, e);
                handleCompletion(false);
            } finally {
                ACTIVE_DELETE_THREAD.compareAndSet(Thread.currentThread(), null);
                IS_DELETING.set(false);
            }
        }, "World-Recycle-Bin-Thread");
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
                        success ? "msg.kineticcore.archive_deleted" : "msg.kineticcore.archive_delete_failed"
                ));
            });
        });
    }

    private static void registerShutdownHookOnce() {
        if (!SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Thread deleteThread = ACTIVE_DELETE_THREAD.get();
            if (deleteThread == null || !deleteThread.isAlive()) {
                return;
            }

            try {
                deleteThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "KineticCore-World-Recycle-Shutdown"));
    }
}
