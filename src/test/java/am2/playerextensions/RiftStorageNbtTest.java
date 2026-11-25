package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.junit.Test;

public class RiftStorageNbtTest {

    @Test
    public void invalidAndDuplicateIndicesDoNotThrow() {
        NBTTagList list = new NBTTagList();
        list.appendTag(entry(-1));
        list.appendTag(entry(54));
        list.appendTag(entry(0));
        list.appendTag(entry(0));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("PlayerRiftStorage", list);

        RiftStorage storage = new RiftStorage();
        storage.loadNBTData(tag);

        assertEquals(54, storage.getSizeInventory());
        assertNull(storage.getStackInSlot(0));
    }

    @Test
    public void emptyStorageWritesAnEmptyList() {
        RiftStorage storage = new RiftStorage();
        NBTTagCompound tag = new NBTTagCompound();
        storage.saveNBTData(tag);

        NBTTagList list = tag.getTagList("PlayerRiftStorage", Constants.NBT.TAG_COMPOUND);
        assertEquals(0, list.tagCount());
    }

    @Test
    public void missingListLoadsAnEmptyInventory() {
        RiftStorage storage = new RiftStorage();
        storage.loadNBTData(new NBTTagCompound());

        assertEquals(54, storage.getSizeInventory());
        assertNull(storage.getStackInSlot(53));
    }

    private static NBTTagCompound entry(int index) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setByte("ArrayIndex", (byte) index);
        return entry;
    }
}
