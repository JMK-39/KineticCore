package dev.xyat.kineticcore.internal.runtime;

import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KineticPlatformRuntime {
    private KineticPlatformRuntime() {
    }

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Path configFile(String relativeFile) {
        return resolveConfigFile(relativeFile);
    }

    public static Path ensureConfigDirectory(String relativeDirectory) throws IOException {
        Path relative = Path.of(relativeDirectory.trim());
        if (relativeDirectory.isBlank() || relative.isAbsolute()) {
            throw new IllegalArgumentException("relativeDirectory must be a non-empty relative path");
        }
        Path root = configDirectory().toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("relativeDirectory escapes the configuration directory");
        }
        return Files.createDirectories(target);
    }

    public static Path writeConfigText(String relativeFile, String content) throws IOException {
        Path target = resolveConfigFile(relativeFile);
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    public static String readConfigText(String relativeFile) throws IOException {
        Path target = resolveConfigFile(relativeFile);
        return Files.readString(target, StandardCharsets.UTF_8);
    }

    public static boolean configFileExists(String relativeFile) {
        return Files.isRegularFile(resolveConfigFile(relativeFile));
    }


    public static byte[] readConfigBytes(String relativeFile) throws IOException {
        return Files.readAllBytes(resolveConfigFile(relativeFile));
    }

    public static List<String> readConfigLines(String relativeFile) throws IOException {
        Path target = resolveConfigFile(relativeFile);
        if (!Files.isRegularFile(target)) return List.of();
        return List.copyOf(Files.readAllLines(target, StandardCharsets.UTF_8));
    }

    public static boolean ensureConfigResource(Class<?> resourceOwner, String resourcePath, String relativeFile) throws IOException {
        Path target = resolveConfigFile(relativeFile);
        if (Files.isRegularFile(target)) return false;
        if (Files.exists(target)) {
            throw new IOException("Config path is not a regular file: " + target.toAbsolutePath());
        }
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        String normalizedResource = resourcePath == null ? "" : resourcePath.strip();
        while (normalizedResource.startsWith("/")) normalizedResource = normalizedResource.substring(1);
        if (normalizedResource.isBlank()) throw new IllegalArgumentException("resourcePath must be non-empty");
        ClassLoader loader = resourceOwner.getClassLoader();
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (var input = loader.getResourceAsStream(normalizedResource)) {
            if (input == null) throw new IOException("Bundled resource is missing: " + normalizedResource);
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            moveReplacing(temp, target);
            return true;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public static void writeConfigTextsAtomic(Map<String, String> files) throws IOException {
        if (files == null || files.isEmpty()) return;
        Map<Path, Path> temps = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Path target = resolveConfigFile(entry.getKey());
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(temp, entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8);
                temps.put(target, temp);
            }
            for (Map.Entry<Path, Path> entry : temps.entrySet()) {
                moveReplacing(entry.getValue(), entry.getKey());
            }
        } finally {
            for (Path temp : temps.values()) Files.deleteIfExists(temp);
        }
    }

    private static Path resolveConfigFile(String relativeFile) {
        if (relativeFile == null) throw new IllegalArgumentException("relativeFile must not be null");
        Path relative = Path.of(relativeFile.trim());
        if (relativeFile.isBlank() || relative.isAbsolute() || relative.getFileName() == null) {
            throw new IllegalArgumentException("relativeFile must name a non-empty relative file");
        }
        Path root = configDirectory().toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("relativeFile escapes the configuration directory");
        }
        return target;
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
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
