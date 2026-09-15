package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.network.NetworkChannel;
import dev.xyat.kineticcore.api.network.NetworkVersionPolicy;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class KineticNetworkRuntime {
    private static final Map<ResourceLocation, ChannelRegistration> CHANNELS = new LinkedHashMap<>();

    private KineticNetworkRuntime() {
    }

    public static synchronized NetworkChannel channel(
            ResourceLocation id,
            String protocolVersion,
            NetworkVersionPolicy versionPolicy
    ) {
        Objects.requireNonNull(id, "id");
        String version = Objects.requireNonNull(protocolVersion, "protocolVersion").trim();
        NetworkVersionPolicy policy = Objects.requireNonNull(versionPolicy, "versionPolicy");
        if (version.isEmpty()) {
            throw new IllegalArgumentException("protocolVersion cannot be blank");
        }

        ChannelRegistration existing = CHANNELS.get(id);
        if (existing != null) {
            if (!existing.protocolVersion().equals(version) || existing.versionPolicy() != policy) {
                throw new IllegalStateException(
                        "Network channel " + id + " is already registered with protocol "
                                + existing.protocolVersion() + " and policy " + existing.versionPolicy()
                                + ", requested " + version + " and policy " + policy
                );
            }
            return existing.channel();
        }

        NetworkChannel channel = new ForgeNetworkChannel(id, version, policy);
        CHANNELS.put(id, new ChannelRegistration(version, policy, channel));
        return channel;
    }

    private record ChannelRegistration(
            String protocolVersion,
            NetworkVersionPolicy versionPolicy,
            NetworkChannel channel
    ) {
    }
}
