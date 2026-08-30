package dev.xyat.kineticcore.api;

import net.minecraftforge.network.NetworkRegistry;

/**
 * Shared protocol-version policy for KineticCore network channels.
 */
public final class KTNetworkProtocol {
    private KTNetworkProtocol() {
    }

    /**
     * Accepts every version advertised by an installed remote channel.
     * Missing channels and vanilla endpoints remain rejected because callers
     * send packets that require the corresponding channel to exist.
     */
    public static boolean acceptsAnyVersion(String remoteVersion) {
        return remoteVersion != null
                && !NetworkRegistry.ABSENT.version().equals(remoteVersion)
                && !NetworkRegistry.ACCEPTVANILLA.equals(remoteVersion);
    }
}
