package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import am2.api.spell.enums.SkillPointTypes;
import am2.api.spell.enums.SkillTrees;

public class SkillDataNbtTest {

    @Test
    public void spellPointsRoundTrip() {
        SkillData original = new SkillData(mock(EntityPlayer.class));
        original.setSpellPoints(7, 8, 9);

        NBTTagCompound tag = new NBTTagCompound();
        original.saveNBTData(tag);

        SkillData loaded = new SkillData(mock(EntityPlayer.class));
        loaded.loadNBTData(tag);

        assertEquals(7, loaded.getSpellPoints(SkillPointTypes.BLUE));
        assertEquals(8, loaded.getSpellPoints(SkillPointTypes.GREEN));
        assertEquals(9, loaded.getSpellPoints(SkillPointTypes.RED));
        assertEquals(SkillTrees.None, loaded.getPrimaryTree());
    }

    @Test
    public void corruptSpellPointsFallBackToTheDefault() {
        NBTTagCompound knowledge = new NBTTagCompound();
        knowledge.setIntArray("SpellPoints", new int[] { 1, 2 });
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SpellKnowledge", knowledge);

        SkillData loaded = new SkillData(mock(EntityPlayer.class));
        loaded.loadNBTData(tag);

        assertEquals(3, loaded.getSpellPoints(SkillPointTypes.BLUE));
        assertEquals(0, loaded.getSpellPoints(SkillPointTypes.GREEN));
        assertEquals(0, loaded.getSpellPoints(SkillPointTypes.RED));
    }

    @Test
    public void invalidPrimaryTreeFallsBackToNone() {
        NBTTagCompound knowledge = new NBTTagCompound();
        knowledge.setIntArray("SpellPoints", new int[] { 3, 0, 0, 0 });
        knowledge.setInteger("PrimarySkillTree", 999);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SpellKnowledge", knowledge);

        SkillData loaded = new SkillData(mock(EntityPlayer.class));
        loaded.loadNBTData(tag);

        assertEquals(SkillTrees.None, loaded.getPrimaryTree());
    }

    @Test
    public void missingKnowledgeTagLeavesTheDefaults() {
        SkillData loaded = new SkillData(mock(EntityPlayer.class));
        loaded.loadNBTData(new NBTTagCompound());

        assertEquals(3, loaded.getSpellPoints(SkillPointTypes.BLUE));
        assertEquals(SkillTrees.None, loaded.getPrimaryTree());
    }
}
