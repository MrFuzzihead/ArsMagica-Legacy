package am2.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the top-level payload bound applied before the server/client processors allocate and dispatch a packet.
 */
public class PacketValidationTest {

    @Test
    public void acceptsBoundsInsideTheLimit() {
        assertTrue(PacketValidation.isValidPayloadLength(0));
        assertTrue(PacketValidation.isValidPayloadLength(1));
        assertTrue(PacketValidation.isValidPayloadLength(PacketValidation.MAX_PAYLOAD_BYTES));
        assertTrue(PacketValidation.isValidPayload(new byte[PacketValidation.MAX_PAYLOAD_BYTES]));
    }

    @Test
    public void rejectsNegativeAndOversizedLengths() {
        assertFalse(PacketValidation.isValidPayloadLength(-1));
        assertFalse(PacketValidation.isValidPayloadLength(PacketValidation.MAX_PAYLOAD_BYTES + 1));
    }

    @Test
    public void rejectsNullPayloads() {
        assertFalse(PacketValidation.isValidPayload(null));
    }
}
