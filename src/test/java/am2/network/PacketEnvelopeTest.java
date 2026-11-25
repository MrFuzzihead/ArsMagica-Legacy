package am2.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

public class PacketEnvelopeTest {

    @Test
    public void validFrameCarriesIdAndPayload() throws Exception {
        byte[] frame = new byte[] { AMPacketIDs.SPELL_CAST, 1, 2, 3 };
        PacketEnvelope envelope = PacketEnvelope.read(input(frame), Side.CLIENT);

        assertNotNull(envelope);
        assertEquals(AMPacketIDs.SPELL_CAST, envelope.id());
        assertArrayEquals(new byte[] { 1, 2, 3 }, envelope.payload());
    }

    @Test
    public void emptyPayloadIsValid() throws Exception {
        byte[] frame = new byte[] { AMPacketIDs.SPELL_CAST };
        PacketEnvelope envelope = PacketEnvelope.read(input(frame), Side.CLIENT);

        assertNotNull(envelope);
        assertEquals(0, envelope.payload().length);
    }

    @Test
    public void wrongSideAndUnknownIdsAreRejected() throws Exception {
        assertNull(PacketEnvelope.read(input(new byte[] { AMPacketIDs.SPELL_CAST }), Side.SERVER));
        assertNull(PacketEnvelope.read(input(new byte[] { (byte) 99 }), Side.CLIENT));
    }

    @Test
    public void oversizedPayloadIsRejectedBeforeAllocation() throws Exception {
        byte[] frame = new byte[PacketValidation.MAX_PAYLOAD_BYTES + 2];
        frame[0] = AMPacketIDs.SPELL_CAST;

        assertNull(PacketEnvelope.read(input(frame), Side.CLIENT));
    }

    @Test
    public void nullInputOrSideIsRejected() throws Exception {
        assertNull(PacketEnvelope.read(null, Side.CLIENT));
        assertNull(PacketEnvelope.read(input(new byte[] { AMPacketIDs.SPELL_CAST }), null));
    }

    private static ByteBufInputStream input(byte[] frame) {
        return new ByteBufInputStream(Unpooled.wrappedBuffer(frame));
    }
}
