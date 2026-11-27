package am2.power;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.junit.Test;

import am2.api.math.AMVector3;
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
        NBTTagList nodePaths = pathEntry.getTagList("nodePaths", Constants.NBT.TAG_LIST);
        assertEquals(2, nodePaths.tagCount());

        List<LinkedList<AMVector3>> loaded = entry.getNodePaths()
            .get(PowerTypes.LIGHT);
        assertEquals(2, loaded.size());
        assertEquals(
            1.0F,
            loaded.get(0)
                .get(0).x,
            0.001F);
        assertEquals(
            4.0F,
            loaded.get(1)
                .get(0).x,
            0.001F);
    }

    @Test
    public void saveAndReadRoundTripsMultiplePathsInOrder() {
        PowerNodeEntry original = new PowerNodeEntry();
        original.setPower(PowerTypes.LIGHT, 12.0F);
        original.registerNodePath(PowerTypes.LIGHT, path(1.0F, 2.0F, 3.0F));
        original.registerNodePath(PowerTypes.LIGHT, path(4.0F, 5.0F, 6.0F));

        PowerNodeEntry loaded = new PowerNodeEntry();
        loaded.readFromNBT(original.saveToNBT());

        assertEquals(12.0F, loaded.getPower(PowerTypes.LIGHT), 0.001F);
        List<LinkedList<AMVector3>> paths = loaded.getNodePaths()
            .get(PowerTypes.LIGHT);
        assertEquals(2, paths.size());
        assertEquals(
            1.0F,
            paths.get(0)
                .getFirst().x,
            0.001F);
        assertEquals(
            4.0F,
            paths.get(1)
                .getFirst().x,
            0.001F);
    }

    private static LinkedList<AMVector3> path(float x, float y, float z) {
        LinkedList<AMVector3> path = new LinkedList<AMVector3>();
        path.add(new AMVector3(x, y, z));
        return path;
    }

    private static NBTTagCompound node(float x, float y, float z) {
        NBTTagCompound node = new NBTTagCompound();
        node.setFloat("Vec3_x", x);
        node.setFloat("Vec3_y", y);
        node.setFloat("Vec3_z", z);
        return node;
    }

    private static NBTTagCompound entryTag() {
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList powerAmounts = new NBTTagList();
        NBTTagCompound amount = new NBTTagCompound();
        amount.setInteger("powerType", PowerTypes.LIGHT.ID());
        amount.setFloat("powerAmount", 42.0F);
        powerAmounts.appendTag(amount);
        tag.setTag("powerAmounts", powerAmounts);

        NBTTagList firstPath = new NBTTagList();
        firstPath.appendTag(node(1.0F, 2.0F, 3.0F));
        NBTTagList secondPath = new NBTTagList();
        secondPath.appendTag(node(4.0F, 5.0F, 6.0F));

        NBTTagList nodePaths = new NBTTagList();
        nodePaths.appendTag(firstPath);
        nodePaths.appendTag(secondPath);

        NBTTagCompound pathEntry = new NBTTagCompound();
        pathEntry.setInteger("powerType", PowerTypes.LIGHT.ID());
        pathEntry.setTag("nodePaths", nodePaths);

        NBTTagList powerPathList = new NBTTagList();
        powerPathList.appendTag(pathEntry);
        tag.setTag("powerPathList", powerPathList);
        return tag;
    }
}
