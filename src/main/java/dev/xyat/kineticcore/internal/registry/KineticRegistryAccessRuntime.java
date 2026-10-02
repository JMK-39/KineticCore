package dev.xyat.kineticcore.internal.registry;

import dev.xyat.kineticcore.internal.client.KineticClientRuntimeImpl;
import dev.xyat.kineticcore.internal.runtime.KineticEnvironmentRuntime;
import dev.xyat.kineticcore.internal.runtime.KineticServerRuntimeImpl;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

/** The registries of the world that is running here: the server's, else the client's. */
public final class KineticRegistryAccessRuntime {
    private KineticRegistryAccessRuntime() {
    }

    /** Returns the loaded world's registries, or {@code null} outside a world. */
    public static RegistryAccess current() {
        MinecraftServer server = KineticServerRuntimeImpl.currentServer();
        if (server != null) return server.registryAccess();
        return KineticEnvironmentRuntime.callOnClient(() -> KineticClientRuntimeImpl::registryAccess, null);
    }

    /** Returns the loaded world's registries, or only the built-in ones outside a world. */
    public static RegistryAccess currentOrBuiltIn() {
        RegistryAccess access = current();
        return access != null ? access : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
}
