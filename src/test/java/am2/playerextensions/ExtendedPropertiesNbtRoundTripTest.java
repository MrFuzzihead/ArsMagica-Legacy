package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMCore;
import am2.api.ArsMagicaApi;
import am2.api.spell.enums.ContingencyTypes;
import am2.configuration.AMConfig;

public class ExtendedPropertiesNbtRoundTripTest {

    private AMConfig previousConfig;

    @Before
    public void installConfig() {
        previousConfig = AMCore.config;
        AMCore.config = mock(AMConfig.class);
        when(AMCore.config.getManaCap()).thenReturn(0.0);
    }

    @After
    public void restoreConfig() {
        AMCore.config = previousConfig;
    }

    private static EntityLivingBase entity() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        BaseAttributeMap attributes = mock(BaseAttributeMap.class);
        IAttributeInstance mana = mock(IAttributeInstance.class);
        IAttributeInstance fatigue = mock(IAttributeInstance.class);
        World world = mock(World.class);
        world.isRemote = true;
        entity.worldObj = world;
        when(entity.getAttributeMap()).thenReturn(attributes);
        when(attributes.getAttributeInstance(ArsMagicaApi.maxManaBonus)).thenReturn(mana);
        when(attributes.getAttributeInstance(ArsMagicaApi.maxBurnoutBonus)).thenReturn(fatigue);
        when(mana.getAttributeValue()).thenReturn(0.0D);
        when(fatigue.getAttributeValue()).thenReturn(0.0D);
        return entity;
    }

    @Test
    public void fullStateRoundTrips() {
        ExtendedProperties original = new ExtendedProperties();
        original.setEntityReference(entity());
        original.setMagicLevelWithMana(10);
        original.setCurrentMana(1234.0F);
        original.setCurrentFatigue(56.0F);
        original.addToExtraVariables("karma", "12");
        original.setCompendiumEntryNoSync("entry_a", "U");
        original.setContingency(ContingencyTypes.FALL, new ItemStack(Items.snowball, 2));
        original.setMarkLocation(1.5D, 2.5D, 3.5D, 7);

        NBTTagCompound tag = new NBTTagCompound();
        original.saveNBTData(tag);

        ExtendedProperties loaded = new ExtendedProperties();
        loaded.setEntityReference(entity());
        loaded.loadNBTData(tag);

        assertEquals(10, loaded.getMagicLevel());
        assertEquals(original.getCurrentMana(), loaded.getCurrentMana(), 0.001F);
        assertEquals(original.getCurrentFatigue(), loaded.getCurrentFatigue(), 0.001F);
        assertEquals("12", loaded.getExtraVariable("karma"));
        assertTrue(loaded.hasCompendiumEntry("entry_a"));
        assertEquals(
            "U",
            loaded.getAllCompendiumEntries()
                .get("entry_a"));
        assertEquals(7, loaded.getMarkDimension());
        assertTrue(loaded.getMarkSet());
        assertEquals(1.5D, loaded.getMarkX(), 0.001D);
        assertEquals(2.5D, loaded.getMarkY(), 0.001D);
        assertEquals(3.5D, loaded.getMarkZ(), 0.001D);

        ItemStack contingency = loaded.getContingencyEffect(2);
        assertNotNull(contingency);
        assertEquals(Items.snowball, contingency.getItem());
    }
}
