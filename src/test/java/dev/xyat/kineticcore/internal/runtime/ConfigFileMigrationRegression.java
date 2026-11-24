package dev.xyat.kineticcore.internal.runtime;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

public final class ConfigFileMigrationRegression {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("kinetic-config-migration-");
        try {
            Path old = root.resolve("settings.toml"), target = root.resolve("mod/settings.toml");
            byte[] original = {1, 0, (byte) 0xFF, 13, 10};
            Files.write(old, original);
            migrate("file", root, old, target);
            require(!Files.exists(old) && Arrays.equals(original, Files.readAllBytes(target)), "moves exact bytes before reading defaults");
            migrate("file", root, old, target);
            Files.writeString(old, "older override");
            migrate("file", root, old, target);
            require(Files.readString(old).equals("older override") && Arrays.equals(original, Files.readAllBytes(target)), "new file wins and both files survive a conflict");

            Path dir = root.resolve("old-dir"), replacement = root.resolve("mod/data");
            Files.createDirectories(dir.resolve("zh_cn"));
            Files.writeString(dir.resolve("zh_cn/edit.json"), "custom language");
            Files.writeString(dir.resolve("conflict.json"), "old");
            Files.createDirectories(replacement);
            Files.writeString(replacement.resolve("conflict.json"), "new");
            migrate("directory", root, dir, replacement);
            require(!Files.exists(dir.resolve("zh_cn")) && Files.readString(replacement.resolve("zh_cn/edit.json")).equals("custom language"), "moves arbitrary nested user files");
            require(Files.readString(dir.resolve("conflict.json")).equals("old") && Files.readString(replacement.resolve("conflict.json")).equals("new"), "directory merge preserves conflicting files");
            try {
                migrate("file", root, old, root.resolve("../escaped.toml"));
                throw new AssertionError("out-of-tree move accepted");
            } catch (IllegalArgumentException expected) { }
            try {
                migrate("directory", root, dir, dir.resolve("new"));
                throw new AssertionError("nested directory move accepted");
            } catch (IllegalArgumentException expected) { }
        } finally {
            try (var walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("CONFIG_FILE_MIGRATION_PASS");
    }

    private static void migrate(String method, Path root, Path from, Path to) throws Exception {
        try {
            var operation = Class.forName("dev.xyat.kineticcore.internal.runtime.ConfigFileMigration")
                    .getDeclaredMethod(method, Path.class, Path.class, Path.class);
            operation.setAccessible(true);
            operation.invoke(null, root, from, to);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            throw error;
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
