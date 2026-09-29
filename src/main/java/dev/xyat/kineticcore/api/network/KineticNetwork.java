package dev.xyat.kineticcore.api.network;

import dev.xyat.kineticcore.internal.network.KineticNetworkRuntime;
import net.minecraft.resources.ResourceLocation;

/** Entry point for Kinetic network channels and the game-wide transport limits. */
public final class KineticNetwork {
    private static volatile NetworkTransportLimits transportLimits = NetworkTransportLimits.DEFAULT;

    private KineticNetwork() {
    }

    /**
     * Creates a channel, or returns the existing one with the same id. Call during mod construction or common
     * setup.
     *
     * @param id channel id, for example {@code mymod:main}
     * @param protocolVersion protocol version compared with the other side; not blank
     * @param versionPolicy {@link NetworkVersionPolicy#EXACT} to refuse connections with a different or missing
     *   version, {@link NetworkVersionPolicy#ANY} to accept any
     * @return the channel
     * @throws NullPointerException if an argument is {@code null}
     * @throws IllegalArgumentException if {@code protocolVersion} is blank
     * @throws IllegalStateException if the id is already registered with a different version or policy
     */
    public static NetworkChannel channel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        return KineticNetworkRuntime.channel(id, protocolVersion, versionPolicy);
    }

    /** Returns the transport limits currently applied to vanilla packet decoding. */
    public static NetworkTransportLimits transportLimits() {
        return transportLimits;
    }

    /**
     * Replaces the transport limits applied to vanilla packet decoding. Takes effect for packets decoded
     * afterwards; KineticCore's network-limit feature calls it from its config.
     *
     * @param limits new limits
     * @throws IllegalArgumentException if {@code limits} is {@code null}
     */
    public static void configureTransportLimits(NetworkTransportLimits limits) {
        if (limits == null) throw new IllegalArgumentException("limits");
        transportLimits = limits;
    }
}
