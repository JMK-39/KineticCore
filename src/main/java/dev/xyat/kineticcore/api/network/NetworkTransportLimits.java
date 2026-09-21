package dev.xyat.kineticcore.api.network;

/** Immutable network transport limits data exposed by this API. */
public record NetworkTransportLimits(
        int timeoutSeconds,
        int customPayloadBytes,
        int decoderBytes,
        int chunkPacketBytes,
        long nbtBytes,
        int stringChars,
        int varIntBytes,
        int varLongBytes,
        int varInt21Bytes
) {
    /**
     * Creates a new network transport limits instance.
     */
    public static final NetworkTransportLimits DEFAULT = new NetworkTransportLimits(
            30,
            1_048_576,
            8_388_608,
            2_097_152,
            2_097_152L,
            32_767,
            5,
            10,
            3
    );

    /**
     * Validates and normalizes this network transport limits value.
     */
    public NetworkTransportLimits {
        requirePositive(timeoutSeconds, "timeoutSeconds");
        requirePositive(customPayloadBytes, "customPayloadBytes");
        requirePositive(decoderBytes, "decoderBytes");
        requirePositive(chunkPacketBytes, "chunkPacketBytes");
        if (nbtBytes <= 0L) throw new IllegalArgumentException("nbtBytes must be positive");
        requirePositive(stringChars, "stringChars");
        requirePositive(varIntBytes, "varIntBytes");
        requirePositive(varLongBytes, "varLongBytes");
        requirePositive(varInt21Bytes, "varInt21Bytes");
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
}
