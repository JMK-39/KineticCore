package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticPlatformRuntime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Public Kinetic API facade for paths. */
public final class KineticPaths {
    private KineticPaths() {
    }

    /**
     * Performs the config directory API operation.
     */
    public static Path configDirectory() {
        return KineticPlatformRuntime.configDirectory();
    }

    /**
     * Performs the game directory API operation.
     */
    public static Path gameDirectory() {
        return KineticPlatformRuntime.gameDirectory();
    }

    /**
     * Resolves one file path below the shared configuration directory without touching the file system.
     *
     * @param relativeFile relative file such as {@code "examplemod/settings.toml"}
     * @return normalized path inside the shared configuration directory
     * @throws IllegalArgumentException if the supplied path is absolute, blank, names no file, or escapes the configuration directory
     */
    public static Path configFile(String relativeFile) {
        return KineticPlatformRuntime.configFile(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /**
     * Creates a relative subdirectory below the shared configuration directory and returns its normalized path.
     *
     * @param relativeDirectory relative directory such as {@code "examplemod"} or {@code "examplemod/cache"}
     * @return the existing or newly created directory
     * @throws IOException if the directory cannot be created
     * @throws IllegalArgumentException if the supplied path is absolute or escapes the configuration directory
     */
    public static Path ensureConfigDirectory(String relativeDirectory) throws IOException {
        return KineticPlatformRuntime.ensureConfigDirectory(Objects.requireNonNull(relativeDirectory, "relativeDirectory"));
    }

    /**
     * Writes one UTF-8 text file below the shared configuration directory, creating parent directories as needed.
     *
     * @param relativeFile relative file such as {@code "examplemod/export.txt"}
     * @param content text content to write; {@code null} is written as an empty string
     * @return the normalized file path that was written
     * @throws IOException if the parent directory or file cannot be written
     * @throws IllegalArgumentException if the supplied path is absolute, blank, names no file, or escapes the configuration directory
     */
    public static Path writeConfigText(String relativeFile, String content) throws IOException {
        return KineticPlatformRuntime.writeConfigText(
                Objects.requireNonNull(relativeFile, "relativeFile"),
                content == null ? "" : content
        );
    }

    /**
     * Reads one UTF-8 text file below the shared configuration directory as individual lines.
     * Missing files return an empty list so callers can preserve optional-file behavior without direct filesystem access.
     *
     * @param relativeFile relative file such as {@code "examplemod/config.toml"}
     * @return immutable text lines, or an empty list when the file does not exist
     * @throws IOException if an existing file cannot be read
     * @throws IllegalArgumentException if the supplied path is absolute, blank, names no file, or escapes the configuration directory
     */
    public static List<String> readConfigLines(String relativeFile) throws IOException {
        return KineticPlatformRuntime.readConfigLines(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /** Reads one existing UTF-8 text file below the shared configuration directory. */
    public static String readConfigText(String relativeFile) throws IOException {
        return KineticPlatformRuntime.readConfigText(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /** Returns whether one regular file exists below the shared configuration directory. */
    public static boolean configFileExists(String relativeFile) {
        return KineticPlatformRuntime.configFileExists(Objects.requireNonNull(relativeFile, "relativeFile"));
    }


    /** Reads the exact bytes of one existing file below the shared configuration directory. */
    public static byte[] readConfigBytes(String relativeFile) throws IOException {
        return KineticPlatformRuntime.readConfigBytes(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /**
     * Copies one bundled classpath resource into the configuration directory only when the target file is missing.
     * Existing regular files are preserved; an existing non-file path is rejected.
     *
     * @return {@code true} when a new config file was created
     */
    public static boolean ensureConfigResource(Class<?> resourceOwner, String resourcePath, String relativeFile) throws IOException {
        return KineticPlatformRuntime.ensureConfigResource(
                Objects.requireNonNull(resourceOwner, "resourceOwner"),
                Objects.requireNonNull(resourcePath, "resourcePath"),
                Objects.requireNonNull(relativeFile, "relativeFile")
        );
    }

    /**
     * Writes multiple UTF-8 configuration files through sibling temporary files before replacement.
     * All supplied keys are relative to the shared configuration directory.
     */
    public static void writeConfigTextsAtomic(Map<String, String> files) throws IOException {
        Map<String, String> safeFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : Objects.requireNonNull(files, "files").entrySet()) {
            safeFiles.put(Objects.requireNonNull(entry.getKey(), "relativeFile"), entry.getValue() == null ? "" : entry.getValue());
        }
        KineticPlatformRuntime.writeConfigTextsAtomic(safeFiles);
    }
}
