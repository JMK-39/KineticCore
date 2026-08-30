package dev.xyat.kineticcore.feature.logcleaner;

import dev.xyat.kineticcore.KineticCore;
import dev.xyat.kineticcore.bootstrap.annotation.KTModule;
import dev.xyat.kineticcore.feature.logcleaner.config.LogCleanerConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@KTModule
public class LogCleanerModule {
    private static boolean shutdownHookInstalled;

    public static void load() {
        LogCleanerConfig.load();
        DuplicateLogFilter.inject();
        if (shutdownHookInstalled) return;
        shutdownHookInstalled = true;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!LogCleanerConfig.enableCleanup) return;

            cleanDirectory(FMLPaths.GAMEDIR.get().resolve("crash-reports"), LogCleanerConfig.maxCrashReports,
                    p -> true);

            Path logsDir = FMLPaths.GAMEDIR.get().resolve("logs");
            if (Files.exists(logsDir)) {
                cleanDirectory(logsDir, LogCleanerConfig.maxLogs, p -> {
                    String name = p.getFileName().toString();
                    return !name.equals("latest.log") && !name.equals("debug.log") && !name.startsWith("debug-");
                });

                cleanDirectory(logsDir, LogCleanerConfig.maxDebugLogs, p -> {
                    String name = p.getFileName().toString();
                    return name.startsWith("debug-") && !name.equals("debug.log");
                });
            }

        }, "KineticCore-LogCleaner-Async-Cleaner"));
    }

    private static void cleanDirectory(Path targetDir, int maxKeep, Predicate<Path> filter) {
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) return;

        try (Stream<Path> paths = Files.list(targetDir)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(filter)
                    .sorted(Comparator.comparing((Path p) -> {
                        try {
                            return Files.readAttributes(p, BasicFileAttributes.class).lastModifiedTime().toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed())
                    .toList();

            if (files.size() > maxKeep) {
                for (int i = maxKeep; i < files.size(); i++) {
                    Path fileToDelete = files.get(i);
                    try {
                        Files.delete(fileToDelete);
                    } catch (IOException e) {
                        System.err.println("[KineticCore/LogCleaner] Failed to delete file: " + fileToDelete.getFileName());
                    }
                }
            }
        } catch (IOException e) {
            KineticCore.LOGGER.error("[LogCleaner] Failed to access directory: {}", targetDir, e);
        }
    }
}