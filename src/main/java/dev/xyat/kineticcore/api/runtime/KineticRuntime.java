package dev.xyat.kineticcore.api.runtime;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** KineticCore's own mod id, logger and id helper. */
public final class KineticRuntime {
    /** KineticCore's mod id. */
    public static final String MOD_ID = "kineticcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    private KineticRuntime() {
    }

    /** Returns KineticCore's logger. Add-ons should use their own logger for their messages. */
    public static Logger logger() {
        return LOGGER;
    }

    /**
     * Creates an id in the {@code kineticcore} namespace.
     *
     * @param path id path
     * @return {@code kineticcore:<path>}
     * @throws IllegalArgumentException if the path contains invalid characters
     */
    public static ResourceLocation id(String path) {
        return KineticResourceIds.of(MOD_ID, path);
    }
}
