package am2.network;

/**
 * Shared bounds checks for custom packet payloads.
 *
 * <p>
 * The payload is read into a byte array before dispatch, so the length must be validated first. This keeps a
 * client-originated packet from forcing an arbitrary allocation before the per-handler authority checks run.
 * </p>
 */
public final class PacketValidation {

    /** Matches the maximum NBT payload accepted by {@link AMDataReader}. */
    public static final int MAX_PAYLOAD_BYTES = 1 << 20;

    private PacketValidation() {}

    public static boolean isValidPayloadLength(int length) {
        return length >= 0 && length <= MAX_PAYLOAD_BYTES;
    }

    public static boolean isValidPayload(byte[] payload) {
        return payload != null && isValidPayloadLength(payload.length);
    }
}
