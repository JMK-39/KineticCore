package dev.xyat.kineticcore.internal.network;

import dev.xyat.kineticcore.api.network.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class KineticNetworkRuntime {
    private static final Map<ResourceLocation, ChannelRegistration> CHANNELS = new LinkedHashMap<>();

    private KineticNetworkRuntime() {
    }

    public static synchronized NetworkChannel channel(ResourceLocation id, String protocolVersion) {
        Objects.requireNonNull(id, "id");
        String version = Objects.requireNonNull(protocolVersion, "protocolVersion").trim();
        if (version.isEmpty()) {
            throw new IllegalArgumentException("protocolVersion cannot be blank");
        }

        ChannelRegistration existing = CHANNELS.get(id);
        if (existing != null) {
            if (!existing.protocolVersion().equals(version)) {
                throw new IllegalStateException(
                        "Network channel " + id + " is already registered with protocol "
                                + existing.protocolVersion() + ", requested " + version
                );
            }
            return existing.channel();
        }

        NetworkChannel channel = new ForgeNetworkChannel(id, version);
        CHANNELS.put(id, new ChannelRegistration(version, channel));
        return channel;
    }

    private record ChannelRegistration(String protocolVersion, NetworkChannel channel) {
    }
}
