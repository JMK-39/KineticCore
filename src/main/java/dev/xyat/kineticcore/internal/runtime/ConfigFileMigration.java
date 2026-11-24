package dev.xyat.kineticcore.internal.runtime;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;

final class ConfigFileMigration {
    private static final System.Logger LOG = System.getLogger("KineticCore.ConfigMigration");

    private ConfigFileMigration() {}

    static synchronized void file(Path root, Path old, Path target) throws IOException {
        checkPath(root, old);
        checkPath(root, target);
        if (old.equals(target) || Files.notExists(old, LinkOption.NOFOLLOW_LINKS)) return;
        if (!Files.isRegularFile(old, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Legacy config is not a regular file: " + old);
        checkRealAncestor(root, old);
        checkRealAncestor(root, target.getParent());
        if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            LOG.log(System.Logger.Level.WARNING, "Using {0}; legacy file was retained at {1}", target, old);
            return;
        }
        Files.createDirectories(target.getParent());
        try {
            Files.move(old, target);
            LOG.log(System.Logger.Level.INFO, "Migrated config {0} to {1}", old, target);
        } catch (FileAlreadyExistsException conflict) {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) throw conflict;
            LOG.log(System.Logger.Level.WARNING, "Using {0}; legacy file was retained at {1}", target, old);
        }
    }

    static synchronized void directory(Path root, Path old, Path target) throws IOException {
        checkPath(root, old);
        checkPath(root, target);
        if (old.equals(target) || Files.notExists(old, LinkOption.NOFOLLOW_LINKS)) return;
        if (old.startsWith(target) || target.startsWith(old)) throw new IllegalArgumentException("Config directories overlap");
        if (!Files.isDirectory(old, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Legacy config is not a directory: " + old);
        checkRealAncestor(root, old);
        checkRealAncestor(root, target);
        var entries = listChildrenFirst(old);
        for (Path entry : entries) {
            if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
                checkRealAncestor(root, entry);
                Path destination = target.resolve(old.relativize(entry));
                checkRealAncestor(root, destination);
                Files.createDirectories(destination);
                try {
                    Files.delete(entry);
                } catch (DirectoryNotEmptyException preserved) {
                    // Conflict files and links remain at the old path.
                }
            } else if (Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                file(root, entry, target.resolve(old.relativize(entry)));
            }
        }
    }

    private static java.util.List<Path> listChildrenFirst(Path root) throws IOException {
        try (var entries = Files.walk(root)) {
            return entries.sorted(Comparator.reverseOrder()).toList();
        }
    }

    private static void checkPath(Path root, Path path) {
        if (!path.equals(path.normalize()) || !path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Config migration path is outside the config directory: " + path);
        }
    }

    private static void checkRealAncestor(Path root, Path path) throws IOException {
        Path existing = path;
        while (Files.notExists(existing, LinkOption.NOFOLLOW_LINKS)) existing = existing.getParent();
        if (!existing.toRealPath().equals(root.toRealPath().resolve(root.relativize(existing)))) {
            throw new IOException("Config migration path crosses a linked entry: " + path);
        }
    }
}
