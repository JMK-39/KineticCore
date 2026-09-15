package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.KineticNetworkRuntime;
import net.minecraft.resources.ResourceLocation;

public final class KineticNetwork {
    private static volatile NetworkTransportLimits transportLimits = NetworkTransportLimits.DEFAULT;

    private KineticNetwork() {
    }

    public static NetworkChannel channel(ResourceLocation id) {
        return channel(id, "1");
    }

    public static NetworkChannel channel(ResourceLocation id, String protocolVersion) {
        return channel(id, protocolVersion, NetworkVersionPolicy.EXACT);
    }

    public static NetworkChannel channel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        return KineticNetworkRuntime.channel(id, protocolVersion, versionPolicy);
    }

    public static NetworkTransportLimits transportLimits() {
        return transportLimits;
    }

    public static void configureTransportLimits(NetworkTransportLimits limits) {
        if (limits == null) throw new IllegalArgumentException("limits");
        transportLimits = limits;
    }
}
