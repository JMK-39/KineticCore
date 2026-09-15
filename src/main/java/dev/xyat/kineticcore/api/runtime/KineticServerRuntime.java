package dev.xyat.kineticcore.api.runtime;

import dev.xyat.kineticcore.internal.runtime.KineticServerRuntimeImpl;
import net.minecraft.server.MinecraftServer;

public final class KineticServerRuntime {
    private KineticServerRuntime() {
    }

    public static MinecraftServer currentServer() {
        return KineticServerRuntimeImpl.currentServer();
    }
}
