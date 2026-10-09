package dev.xyat.kineticcore.feature.datapack.recovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/** Resolves only the explicitly reported local pack; no namespace-to-mod guessing. */
public final class DatapackFiles {
    private DatapackFiles() { }
    public record Location(String openPath, String displayPath, String packPath, boolean archive) { }

    public static Location locate(String packId, String resourceId, Path coreRoot, Path worldRoot) {
        try { return locateLocal(packId, resourceId, coreRoot, worldRoot); }
        catch (java.nio.file.InvalidPathException ignored) { return null; }
    }

    private static Location locateLocal(String packId, String resourceId, Path coreRoot, Path worldRoot) {
        if (packId == null || packId.isEmpty() || resourceId == null) return null;
        int colon = resourceId.indexOf(':');
        if (colon <= 0 || resourceId.indexOf("..") >= 0 || packId.indexOf("..") >= 0) return null;
        String namespace = resourceId.substring(0, colon), path = resourceId.substring(colon + 1);
        if (path.startsWith("/") || path.indexOf('\\') >= 0) return null;
        Path root = packId.startsWith("file/") ? (worldRoot == null ? null : worldRoot.resolve("datapacks")) : coreRoot;
        if (root == null) return null;
        root = root.toAbsolutePath().normalize();
        String name = packId.startsWith("file/") ? packId.substring(5) : packId;
        Path pack = root.resolve(name).normalize();
        if (pack.equals(root) || !pack.startsWith(root)) return null;
        String relative = "data/" + namespace + "/" + path;
        if (Files.isRegularFile(pack) && pack.getFileName().toString().endsWith(".zip")) {
            try (ZipFile zip = new ZipFile(pack.toFile())) {
                if (zip.getEntry(relative) != null) return new Location(pack.toString(), pack + "!/" + relative, pack.toString(), true);
            } catch (IOException ignored) { }
            return null;
        }
        if (!Files.isDirectory(pack)) return null;
        Path file = pack.resolve(relative).normalize();
        // Core also supports a loose namespace folder with no data/ wrapper.
        if (!Files.isRegularFile(file) && !packId.startsWith("file/") && name.equals(namespace)
                && !Files.exists(pack.resolve("data")) && !Files.exists(pack.resolve("pack.mcmeta"))) {
            file = pack.resolve(path).normalize();
        }
        if (!file.startsWith(pack) || !Files.isRegularFile(file)) return null;
        return new Location(file.toString(), file.toString(), pack.toString(), false);
    }
}
