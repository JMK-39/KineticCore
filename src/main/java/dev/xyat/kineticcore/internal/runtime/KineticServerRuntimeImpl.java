package dev.xyat.kineticcore.internal.runtime;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class KineticServerRuntimeImpl {
    private KineticServerRuntimeImpl() {
    }

    public static MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
