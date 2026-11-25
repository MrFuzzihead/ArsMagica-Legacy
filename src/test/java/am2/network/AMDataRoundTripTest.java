package am2.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class AMDataRoundTripTest {

    @Test
    public void everyPrimitiveArrayAndNbtTypeRoundTrips() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("key", "value");
        tag.setInteger("number", 42);

        AMDataWriter writer = new AMDataWriter();
        writer.add(123456)
            .add(true)
            .add((byte) 7)
            .add("hello")
            .add((short) 321)
            .add(12.5D)
            .add(3.25F)
            .add(9876543210L)
            .add(new int[] { 5, 6, 7 })
            .add(tag)
            .add(new byte[] { 1, 2, 3, 4 });

        AMDataReader reader = new AMDataReader(writer.generate(), false);
        assertEquals(123456, reader.getInt());
        assertTrue(reader.getBoolean());
        assertEquals(7, reader.getByte());
        assertEquals("hello", reader.getString());
        assertEquals(321, reader.getShort());
        assertEquals(12.5D, reader.getDouble(), 0.0001D);
        assertEquals(3.25F, reader.getFloat(), 0.0001F);
        assertEquals(9876543210L, reader.getLong());
        assertArrayEquals(new int[] { 5, 6, 7 }, reader.getIntArray());

        NBTTagCompound loaded = reader.getNBTTagCompound();
        assertNotNull(loaded);
        assertEquals("value", loaded.getString("key"));
        assertEquals(42, loaded.getInteger("number"));
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, reader.getRemainingBytes());
        assertFalse(reader.hasFailed());
    }

    @Test
    public void everyScalarGetterMarksTruncatedInputAsFailed() {
        assertTrue(truncated(AMDataReader::getInt).hasFailed());
        assertTrue(truncated(AMDataReader::getFloat).hasFailed());
        assertTrue(truncated(AMDataReader::getDouble).hasFailed());
        assertTrue(truncated(AMDataReader::getBoolean).hasFailed());
        assertTrue(truncated(AMDataReader::getString).hasFailed());
        assertTrue(truncated(AMDataReader::getByte).hasFailed());
        assertTrue(truncated(AMDataReader::getShort).hasFailed());
        assertTrue(truncated(AMDataReader::getLong).hasFailed());
        assertTrue(truncated(AMDataReader::getIntArray).hasFailed());
        assertTrue(truncated(AMDataReader::getNBTTagCompound).hasFailed());
    }

    private static AMDataReader truncated(ReaderCall call) {
        AMDataReader reader = new AMDataReader(new byte[0], false);
        call.read(reader);
        return reader;
    }

    @FunctionalInterface
    private interface ReaderCall {

        void read(AMDataReader reader);
    }
}
