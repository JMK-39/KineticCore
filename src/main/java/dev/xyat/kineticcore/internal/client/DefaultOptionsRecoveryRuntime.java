package dev.xyat.kineticcore.internal.client;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.runtime.KineticFeatureSwitches;
import dev.xyat.kineticcore.api.runtime.KineticPlatform;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

final class DefaultOptionsRecoveryRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FEATURE_ID = "client.default_options";

    private DefaultOptionsRecoveryRuntime() {
    }

    static void restoreConfiguredDefaults() {
        if (!KineticFeatureSwitches.isEnabled(FEATURE_ID)) return;

        Path defaultsFile = KineticPlatform.configDirectory().resolve("kineticcore/defaultoptions.txt");
        Path optionsFile = KineticPlatform.gameDirectory().resolve("options.txt");
        try {
            DefaultOptionsRecovery.Result result = DefaultOptionsRecovery.restore(defaultsFile, optionsFile);
            if (result == DefaultOptionsRecovery.Result.RESTORED_MISSING
                    || result == DefaultOptionsRecovery.Result.RESTORED_DAMAGED) {
                LOGGER.info("Restored client options from {} to {} ({})",
                        defaultsFile, optionsFile,
                        result == DefaultOptionsRecovery.Result.RESTORED_MISSING ? "missing file" : "damaged file");
            } else if (result == DefaultOptionsRecovery.Result.DEFAULTS_MISSING) {
                LOGGER.warn("Client default options are enabled, but the defaults file is missing: {}", defaultsFile);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to restore client options from {} to {}", defaultsFile, optionsFile, exception);
        }
    }
}
