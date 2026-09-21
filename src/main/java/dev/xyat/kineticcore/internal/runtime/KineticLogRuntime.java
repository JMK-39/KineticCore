package dev.xyat.kineticcore.internal.runtime;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Internal runtime owner for Kinetic logging implementation details. */
public final class KineticLogRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();

    private KineticLogRuntime() {
    }

    /** Records an error message together with its cause. */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}
