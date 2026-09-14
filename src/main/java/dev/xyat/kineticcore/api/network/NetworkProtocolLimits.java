package dev.xyat.kineticcore.api.network;

public record NetworkProtocolLimits(
        int maxUtfChars,
        int maxByteArrayBytes,
        int maxCollectionEntries
) {
    public static final NetworkProtocolLimits DEFAULT = new NetworkProtocolLimits(32767, 1024 * 1024, 65536);

    public NetworkProtocolLimits {
        if (maxUtfChars < 1) throw new IllegalArgumentException("maxUtfChars must be positive");
        if (maxByteArrayBytes < 1) throw new IllegalArgumentException("maxByteArrayBytes must be positive");
        if (maxCollectionEntries < 1) throw new IllegalArgumentException("maxCollectionEntries must be positive");
    }
}
