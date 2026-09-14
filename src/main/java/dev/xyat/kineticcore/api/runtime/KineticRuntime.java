package dev.xyat.kineticcore.api.runtime;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class KineticRuntime {
    public static final String MOD_ID = "kineticcore";
    private static final Logger LOGGER = LogUtils.getLogger();

    private KineticRuntime() {
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
