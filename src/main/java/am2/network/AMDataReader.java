package am2.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import am2.LogHelper;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class AMDataReader {

    /** Hard cap for a single NBT payload. Legitimate AM2 packets are far smaller. */
    public static final int MAX_NBT_BYTES = 1 << 20;

    ByteArrayInputStream input;
    DataInputStream dataStream;
    public byte ID;
    private boolean failed;

    public AMDataReader(byte[] data) {
        this(data, true);
    }

    public AMDataReader(byte[] data, boolean getID) {
        input = new ByteArrayInputStream(data);
        dataStream = new DataInputStream(input);

        // get id byte
        if (getID) {
            try {
                ID = dataStream.readByte();
            } catch (IOException e) {
                LogHelper.error("AMDataReader (getID): " + e.toString());
                failed = true;
            }
        }
    }

    public int getInt() {
        int value = 0;
        try {
            value = dataStream.readInt();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getInt): " + e.toString());
            failed = true;
        }
        return value;
    }

    public float getFloat() {
        float value = 0;
        try {
            value = dataStream.readFloat();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getFloat): " + e.toString());
            failed = true;
        }
        return value;
    }

    public double getDouble() {
        double value = 0;
        try {
            value = dataStream.readDouble();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getDouble): " + e.toString());
            failed = true;
        }
        return value;
    }

    public boolean getBoolean() {
        boolean value = false;
        try {
            value = dataStream.readBoolean();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getBoolean): " + e.toString());
            failed = true;
        }
        return value;
    }

    public String getString() {
        String value = "";
        try {
            value = dataStream.readUTF();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getString): " + e.toString());
            failed = true;
        }
        return value;
    }

    public byte getByte() {
        byte value = 0;
        try {
            value = dataStream.readByte();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getByte): " + e.toString());
            failed = true;
        }
        return value;
    }

    public short getShort() {
        short value = 0;
        try {
            value = dataStream.readShort();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getShort): " + e.toString());
            failed = true;
        }
        return value;
    }

    public long getLong() {
        long value = 0;
        try {
            value = dataStream.readLong();
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getLong): " + e.toString());
            failed = true;
        }
        return value;
    }

    public NBTTagCompound getNBTTagCompound() {
        try {
            int len = dataStream.readInt();
            if (len < 0 || len > MAX_NBT_BYTES) {
                LogHelper.error("AMDataReader (getNBTTagCompound): invalid length %d", len);
                failed = true;
                return null;
            }
            byte[] bytes = new byte[len];
            dataStream.readFully(bytes);
            ByteBuf buf = Unpooled.copiedBuffer(bytes);
            return ByteBufUtils.readTag(buf);
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getNBTTagCompound): " + e.toString());
            failed = true;
        }
        return null;
    }

    public boolean hasFailed() {
        return failed;
    }

    public byte[] getRemainingBytes() {
        byte[] remaining = null;
        try {
            remaining = new byte[dataStream.available()];
            dataStream.readFully(remaining);
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getRemainingBytes): " + e.toString());
            failed = true;
        }

        return remaining;
    }

    public ItemStack getItemStack() {
        NBTTagCompound compound = getNBTTagCompound();
        if (compound == null) return null;
        ItemStack stack = ItemStack.loadItemStackFromNBT(compound);
        return stack;
    }

    public int[] getIntArray() {
        try {
            int len = dataStream.readInt();
            if (len < 0 || len > dataStream.available() / 4) {
                LogHelper.error(
                    "AMDataReader (getIntArray): invalid length %d for %d available bytes",
                    len,
                    dataStream.available());
                failed = true;
                return new int[0];
            }
            int[] arr = new int[len];
            for (int i = 0; i < arr.length; ++i) arr[i] = dataStream.readInt();
            return arr;
        } catch (IOException e) {
            LogHelper.error("AMDataReader (getIntArray): " + e.toString());
            failed = true;
        }

        return new int[0];
    }
}
