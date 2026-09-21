package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.KineticNetworkRuntime;
import net.minecraft.resources.ResourceLocation;

/** Public Kinetic API facade for network. */
public final class KineticNetwork {
    private static volatile NetworkTransportLimits transportLimits = NetworkTransportLimits.DEFAULT;

    private KineticNetwork() {
    }

    /**
     * Performs the channel API operation.
     */
    public static NetworkChannel channel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        return KineticNetworkRuntime.channel(id, protocolVersion, versionPolicy);
    }

    /**
     * Performs the transport limits API operation.
     */
    public static NetworkTransportLimits transportLimits() {
        return transportLimits;
    }

    /**
     * Performs the configure transport limits API operation.
     */
    public static void configureTransportLimits(NetworkTransportLimits limits) {
        if (limits == null) throw new IllegalArgumentException("limits");
        transportLimits = limits;
    }
}
