package am2.spell;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import am2.playerextensions.ExtendedProperties;

public class SpellUtilsRequirementsTest {

    @Test
    public void malformedReagentArraysAreIgnoredAndRequirementsAreParsed() throws Exception {
        EntityLivingBase caster = casterWithFatigue(50.0F, 100.0F);
        ItemStack stack = stackWithRequirements(100.0F, 10.0F, new int[] { 1, 2, 1, 5 });

        SpellUtils.SpellRequirements requirements = parse(stack, caster);

        assertEquals(150.0F, requirements.manaCost, 0.001F);
        assertEquals(10.0F, requirements.burnout, 0.001F);
        assertEquals(0, requirements.reagents.size());
    }

    @Test
    public void missingRequirementTagsReadAsZero() throws Exception {
        EntityLivingBase caster = casterWithFatigue(0.0F, 100.0F);
        ItemStack stack = new ItemStack(mock(Item.class));
        stack.setTagCompound(new NBTTagCompound());

        SpellUtils.SpellRequirements requirements = parse(stack, caster);

        assertEquals(0.0F, requirements.manaCost, 0.001F);
        assertEquals(0.0F, requirements.burnout, 0.001F);
        assertEquals(0, requirements.reagents.size());
    }

    @Test
    public void anIncompleteReagentTripleIsDropped() throws Exception {
        EntityLivingBase caster = casterWithFatigue(0.0F, 100.0F);
        ItemStack stack = stackWithRequirements(25.0F, 5.0F, new int[] { 1, 2, 0, 2, 1 });

        SpellUtils.SpellRequirements requirements = parse(stack, caster);

        assertEquals(0, requirements.reagents.size());
        assertEquals(25.0F, requirements.manaCost, 0.001F);
    }

    private static EntityLivingBase casterWithFatigue(float current, float max) {
        EntityLivingBase caster = mock(EntityLivingBase.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(properties.getCurrentFatigue()).thenReturn(current);
        when(properties.getMaxFatigue()).thenReturn(max);
        when(caster.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);
        return caster;
    }

    private static ItemStack stackWithRequirements(float mana, float burnout, int[] reagents) {
        ItemStack stack = new ItemStack(mock(Item.class));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("BMC_", mana);
        tag.setFloat("BB_", burnout);
        tag.setIntArray("BRR", reagents);
        stack.setTagCompound(tag);
        return stack;
    }

    private static SpellUtils.SpellRequirements parse(ItemStack stack, EntityLivingBase caster) throws Exception {
        Method method = SpellUtils.class
            .getDeclaredMethod("parseSpellRequirements", ItemStack.class, EntityLivingBase.class);
        method.setAccessible(true);
        return (SpellUtils.SpellRequirements) method.invoke(SpellUtils.instance, stack, caster);
    }
}
