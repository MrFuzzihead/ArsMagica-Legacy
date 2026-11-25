package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import am2.api.spell.enums.Affinity;

public class AffinityDataNbtTest {

    @Test
    public void affinityDepthsRoundTrip() {
        AffinityData original = new AffinityData();
        original.setAffinityAndDepth(Affinity.AIR, 25.0F);
        original.setAffinityAndDepth(Affinity.FIRE, 50.0F);

        NBTTagCompound tag = new NBTTagCompound();
        original.saveNBTData(tag);

        AffinityData loaded = new AffinityData();
        loaded.loadNBTData(tag);

        assertEquals(0.25F, loaded.getAffinityDepth(Affinity.AIR), 0.001F);
        assertEquals(0.5F, loaded.getAffinityDepth(Affinity.FIRE), 0.001F);
    }

    @Test
    public void lockedAffinityPreventsFurtherIncrements() {
        NBTTagCompound depths = new NBTTagCompound();
        depths.setDouble(Affinity.AIR.name(), 100.0D);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("AffinityDepthData", depths);
        tag.setBoolean("AffinityLocked", true);

        AffinityData loaded = new AffinityData();
        loaded.loadNBTData(tag);
        loaded.incrementAffinity(Affinity.AIR, 10.0F);

        assertEquals(1.0F, loaded.getAffinityDepth(Affinity.AIR), 0.001F);
    }

    @Test
    public void unknownAffinityNamesAreIgnored() {
        NBTTagCompound depths = new NBTTagCompound();
        depths.setDouble("NOT_AN_AFFINITY", 100.0D);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("AffinityDepthData", depths);

        AffinityData loaded = new AffinityData();
        loaded.loadNBTData(tag);

        assertTrue(loaded.getAffinityDepth(Affinity.NONE) == 0.0F);
    }
}
