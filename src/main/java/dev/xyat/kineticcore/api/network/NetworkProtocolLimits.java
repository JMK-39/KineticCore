package dev.xyat.kineticcore.api.network;

/**
 * Default limits used by {@link NetworkBuffer} methods that take no explicit limit.
 *
 * @param maxUtfChars maximum string length in UTF-16 characters
 * @param maxByteArrayBytes maximum byte array length
 * @param maxCollectionEntries maximum number of list or array entries
 */
public record NetworkProtocolLimits(
        int maxUtfChars,
        int maxByteArrayBytes,
        int maxCollectionEntries
) {
    /** 32767 characters, 1 MiB byte arrays and 65536 entries. */
    public static final NetworkProtocolLimits DEFAULT = new NetworkProtocolLimits(32767, 1024 * 1024, 65536);

    /**
     * @throws IllegalArgumentException if any limit is less than 1
     */
    public NetworkProtocolLimits {
        if (maxUtfChars < 1) throw new IllegalArgumentException("maxUtfChars must be positive");
        if (maxByteArrayBytes < 1) throw new IllegalArgumentException("maxByteArrayBytes must be positive");
        if (maxCollectionEntries < 1) throw new IllegalArgumentException("maxCollectionEntries must be positive");
    }
}
