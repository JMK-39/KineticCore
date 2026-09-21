package dev.xyat.kineticcore.api.runtime;

import com.mojang.logging.LogUtils;
import dev.xyat.kineticcore.api.resource.KineticResourceIds;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Public Kinetic API facade for runtime. */
public final class KineticRuntime {
    /**
     * Exposes the mod id API value.
     */
    public static final String MOD_ID = "kineticcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    private KineticRuntime() {
    }

    /**
     * Performs the logger API operation.
     */
    public static Logger logger() {
        return LOGGER;
    }

    /**
     * Returns the id.
     */
    public static ResourceLocation id(String path) {
        return KineticResourceIds.of(MOD_ID, path);
    }
}
