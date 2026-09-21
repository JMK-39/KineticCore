package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticServerRuntimeImpl;
import net.minecraft.server.MinecraftServer;

/** Public Kinetic API facade for server runtime. */
public final class KineticServerRuntime {
    private KineticServerRuntime() {
    }

    /**
     * Returns server.
     */
    public static MinecraftServer currentServer() {
        return KineticServerRuntimeImpl.currentServer();
    }
}
