package dev.xyat.kineticcore.api.network;

/**
 * Size and time limits applied to vanilla packet decoding. Raising them lets large modpacks sync big recipes, NBT
 * or chunks; lowering them hardens servers.
 *
 * @param timeoutSeconds connection read timeout in seconds
 * @param customPayloadBytes maximum custom payload packet size in bytes
 * @param decoderBytes maximum decoded frame size in bytes
 * @param chunkPacketBytes maximum chunk data packet size in bytes
 * @param nbtBytes maximum accounted NBT size in bytes
 * @param stringChars maximum string length in characters
 * @param varIntBytes maximum encoded VarInt length in bytes
 * @param varLongBytes maximum encoded VarLong length in bytes
 * @param varInt21Bytes maximum length of the 21-bit VarInt frame prefix in bytes
 */
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
    /** Limits used until a config changes them; they match or slightly relax the vanilla values. */
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
     * @throws IllegalArgumentException if any limit is zero or negative
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
