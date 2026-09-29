package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticPlatformRuntime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Safe file helpers below the shared config directory. Use {@link KineticPlatform} for the config and game
 * directories themselves.
 *
 * <p>Relative paths are normalized and must stay inside the config directory; absolute paths and {@code ..} escapes
 * are rejected, so values read from configs or packets cannot reach other files.
 */
public final class KineticPaths {
    private KineticPaths() {
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

    /**
     * Reads one existing UTF-8 text file below the shared config directory.
     *
     * @param relativeFile relative file such as {@code "examplemod/notes.txt"}
     * @return the file content
     * @throws IOException if the file is missing or cannot be read
     * @throws IllegalArgumentException if the path is absolute, blank, names no file, or escapes the config
     *   directory
     */
    public static String readConfigText(String relativeFile) throws IOException {
        return KineticPlatformRuntime.readConfigText(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /**
     * Returns whether a regular file exists below the shared config directory.
     *
     * @throws IllegalArgumentException if the path is absolute, blank, names no file, or escapes the config
     *   directory
     */
    public static boolean configFileExists(String relativeFile) {
        return KineticPlatformRuntime.configFileExists(Objects.requireNonNull(relativeFile, "relativeFile"));
    }


    /**
     * Reads the exact bytes of one existing file below the shared config directory.
     *
     * @throws IOException if the file is missing or cannot be read
     * @throws IllegalArgumentException if the path is absolute, blank, names no file, or escapes the config
     *   directory
     */
    public static byte[] readConfigBytes(String relativeFile) throws IOException {
        return KineticPlatformRuntime.readConfigBytes(Objects.requireNonNull(relativeFile, "relativeFile"));
    }

    /**
     * Copies a bundled resource into the config directory only when the target file is missing, for shipping
     * default config files. Existing files are never overwritten.
     *
     * @param resourceOwner class whose class loader loads the resource
     * @param resourcePath classpath path of the default file; a leading {@code /} is optional
     * @param relativeFile target file below the config directory
     * @return {@code true} when a new config file was created
     * @throws IOException if the resource is missing, the target exists but is not a regular file, or writing fails
     * @throws IllegalArgumentException if {@code resourcePath} is blank, or {@code relativeFile} is absolute,
     *   blank, or escapes the config directory
     */
    public static boolean ensureConfigResource(Class<?> resourceOwner, String resourcePath, String relativeFile) throws IOException {
        return KineticPlatformRuntime.ensureConfigResource(
                Objects.requireNonNull(resourceOwner, "resourceOwner"),
                Objects.requireNonNull(resourcePath, "resourcePath"),
                Objects.requireNonNull(relativeFile, "relativeFile")
        );
    }

    /**
     * Writes several UTF-8 files through sibling temporary files and then replaces the targets, so a crash never
     * leaves a half-written file. {@code null} contents are written as empty files.
     *
     * @param files contents keyed by path relative to the config directory
     * @throws IOException if a file cannot be written
     * @throws IllegalArgumentException if a path is absolute, blank, or escapes the config directory
     */
    public static void writeConfigTextsAtomic(Map<String, String> files) throws IOException {
        Map<String, String> safeFiles = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : Objects.requireNonNull(files, "files").entrySet()) {
            safeFiles.put(Objects.requireNonNull(entry.getKey(), "relativeFile"), entry.getValue() == null ? "" : entry.getValue());
        }
        KineticPlatformRuntime.writeConfigTextsAtomic(safeFiles);
    }
}
