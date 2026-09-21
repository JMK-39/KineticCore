package dev.xyat.kineticcore.api.network;

/** Immutable network protocol limits data exposed by this API. */
public record NetworkProtocolLimits(
        int maxUtfChars,
        int maxByteArrayBytes,
        int maxCollectionEntries
) {
    /**
     * Creates a new network protocol limits instance.
     */
    public static final NetworkProtocolLimits DEFAULT = new NetworkProtocolLimits(32767, 1024 * 1024, 65536);

    /**
     * Validates and normalizes this network protocol limits value.
     */
    public NetworkProtocolLimits {
        if (maxUtfChars < 1) throw new IllegalArgumentException("maxUtfChars must be positive");
        if (maxByteArrayBytes < 1) throw new IllegalArgumentException("maxByteArrayBytes must be positive");
        if (maxCollectionEntries < 1) throw new IllegalArgumentException("maxCollectionEntries must be positive");
    }
}
