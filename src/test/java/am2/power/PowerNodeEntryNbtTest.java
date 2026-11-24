package am2.power;

import static org.junit.Assert.assertEquals;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.junit.Test;

import am2.api.power.PowerTypes;

/**
 * Pins the copy-before-read behaviour of {@link PowerNodeEntry#readFromNBT}: the old implementation drained the
 * caller's {@code nodePaths} list with {@code removeTag(0)}.
 */
public class PowerNodeEntryNbtTest {

    @Test
    public void readFromNbtDoesNotDestroyTheCallersList() {
        NBTTagCompound tag = entryTag();

        PowerNodeEntry entry = new PowerNodeEntry();
        entry.readFromNBT(tag);

        assertEquals(42.0F, entry.getPower(PowerTypes.LIGHT), 0.001F);

        NBTTagList powerPathList = tag.getTagList("powerPathList", Constants.NBT.TAG_COMPOUND);
        assertEquals(1, powerPathList.tagCount());

        NBTTagCompound pathEntry = powerPathList.getCompoundTagAt(0);
        NBTTagList nodePaths = (NBTTagList) pathEntry.getTagList("nodePaths", Constants.NBT.TAG_LIST)
            .copy();
        NBTTagList firstPath = (NBTTagList) nodePaths.removeTag(0);
        assertEquals(1, firstPath.tagCount());
    }

    private static NBTTagCompound entryTag() {
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList powerAmounts = new NBTTagList();
        NBTTagCompound amount = new NBTTagCompound();
        amount.setInteger("powerType", PowerTypes.LIGHT.ID());
        amount.setFloat("powerAmount", 42.0F);
        powerAmounts.appendTag(amount);
        tag.setTag("powerAmounts", powerAmounts);

        NBTTagList pathNodes = new NBTTagList();
        NBTTagCompound node = new NBTTagCompound();
        node.setFloat("Vec3_x", 1.0F);
        node.setFloat("Vec3_y", 2.0F);
        node.setFloat("Vec3_z", 3.0F);
        pathNodes.appendTag(node);

        NBTTagList nodePaths = new NBTTagList();
        nodePaths.appendTag(pathNodes);

        NBTTagCompound pathEntry = new NBTTagCompound();
        pathEntry.setInteger("powerType", PowerTypes.LIGHT.ID());
        pathEntry.setTag("nodePaths", nodePaths);

        NBTTagList powerPathList = new NBTTagList();
        powerPathList.appendTag(pathEntry);
        tag.setTag("powerPathList", powerPathList);
        return tag;
    }
}
