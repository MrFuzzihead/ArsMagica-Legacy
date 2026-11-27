package am2.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cpw.mods.fml.relauncher.Side;

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

    @Test
    public void directionRegistryEnforcesClientToServerPackets() {
        assertTrue(PacketValidation.isAllowedDirection(AMPacketIDs.SPELL_SHAPE_GROUP_CHANGE, Side.SERVER));
        assertFalse(PacketValidation.isAllowedDirection(AMPacketIDs.SPELL_SHAPE_GROUP_CHANGE, Side.CLIENT));
    }

    @Test
    public void directionRegistryEnforcesServerToClientPackets() {
        assertTrue(PacketValidation.isAllowedDirection(AMPacketIDs.SPELL_CAST, Side.CLIENT));
        assertFalse(PacketValidation.isAllowedDirection(AMPacketIDs.SPELL_CAST, Side.SERVER));
    }

    @Test
    public void directionRegistryAllowsBidirectionalPackets() {
        assertTrue(PacketValidation.isAllowedDirection(AMPacketIDs.SYNC_EXTENDED_PROPS, Side.CLIENT));
        assertTrue(PacketValidation.isAllowedDirection(AMPacketIDs.SYNC_EXTENDED_PROPS, Side.SERVER));
    }

    @Test
    public void directionRegistryRejectsUnknownIdsAndNullSides() {
        assertFalse(PacketValidation.isAllowedDirection((byte) 99, Side.SERVER));
        assertFalse(PacketValidation.isAllowedDirection(AMPacketIDs.SPELL_CAST, null));
    }
}
