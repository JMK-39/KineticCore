package dev.xyat.kineticcore.internal.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class DefaultOptionsRecovery {
    private static final long MIN_VALID_OPTIONS_SIZE = 2048;

    private DefaultOptionsRecovery() {
    }

    static Result restore(Path defaultsFile, Path optionsFile) throws IOException {
        if (!Files.isRegularFile(defaultsFile)) return Result.DEFAULTS_MISSING;

        boolean optionsExist = Files.isRegularFile(optionsFile);
        if (optionsExist && Files.size(optionsFile) >= MIN_VALID_OPTIONS_SIZE) return Result.PRESERVED;

        Path parent = optionsFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(defaultsFile, optionsFile, StandardCopyOption.REPLACE_EXISTING);
        return optionsExist ? Result.RESTORED_DAMAGED : Result.RESTORED_MISSING;
    }

    enum Result {
        DEFAULTS_MISSING,
        PRESERVED,
        RESTORED_MISSING,
        RESTORED_DAMAGED
    }
}
