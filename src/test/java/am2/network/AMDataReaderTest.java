package am2.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/**
 * Pins the packet-length bounds added to {@link AMDataReader}. A malicious client can put any int in the length
 * prefix; the reader must reject impossible sizes before allocating.
 */
public class AMDataReaderTest {

    @Test
    public void rejectsHugeNbtLength() throws Exception {
        assertNull(reader(intBytes(Integer.MAX_VALUE)).getNBTTagCompound());
    }

    @Test
    public void rejectsNegativeNbtLength() throws Exception {
        assertNull(reader(intBytes(-1)).getNBTTagCompound());
    }

    @Test
    public void rejectsNbtLengthAboveTheCap() throws Exception {
        assertNull(reader(intBytes(AMDataReader.MAX_NBT_BYTES + 1)).getNBTTagCompound());
    }

    @Test
    public void truncatedNbtPayloadReturnsNull() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(4);
        data.write(new byte[] { 0, 1 });
        assertNull(reader(out.toByteArray()).getNBTTagCompound());
    }

    @Test
    public void rejectsHugeIntArrayLength() throws Exception {
        assertArrayEquals(new int[0], reader(intBytes(Integer.MAX_VALUE)).getIntArray());
    }

    @Test
    public void rejectsIntArrayLongerThanThePayload() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(3);
        data.writeInt(1);
        assertArrayEquals(new int[0], reader(out.toByteArray()).getIntArray());
    }

    @Test
    public void validNbtAndIntArrayRoundTrip() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("key", "value");
        AMDataWriter writer = new AMDataWriter();
        writer.add(tag);
        writer.add(new int[] { 1, 2, 3 });

        AMDataReader reader = reader(writer.generate());
        NBTTagCompound loaded = reader.getNBTTagCompound();
        assertNotNull(loaded);
        assertEquals("value", loaded.getString("key"));
        assertArrayEquals(new int[] { 1, 2, 3 }, reader.getIntArray());
    }

    private static AMDataReader reader(byte[] bytes) {
        return new AMDataReader(bytes, false);
    }

    private static byte[] intBytes(int value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(value);
        return out.toByteArray();
    }
}
