package am2.spell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.api.spell.component.interfaces.ISpellModifier;
import am2.api.spell.component.interfaces.ISpellShape;
import am2.items.ItemsCommonProxy;

public class SpellUtilsConstructionTest {

    private SkillManager previousSkillManager;
    private Object previousSpell;
    private SkillManager skillManager;
    private ISpellShape shapeA;
    private ISpellShape shapeB;
    private ISpellModifier modifier;

    @Before
    public void installRegistry() {
        previousSkillManager = SkillManager.instance;
        previousSpell = ItemsCommonProxy.spell;

        skillManager = mock(SkillManager.class);
        shapeA = mock(ISpellShape.class);
        shapeB = mock(ISpellShape.class);
        modifier = mock(ISpellModifier.class);
        when(shapeA.getID()).thenReturn(10);
        when(shapeB.getID()).thenReturn(11);
        when(modifier.getID()).thenReturn(1);

        skillManager.missingShape = mock(ISpellShape.class);
        skillManager.missingModifier = mock(ISpellModifier.class);
        when(skillManager.missingShape.getID()).thenReturn(999);
        when(skillManager.getSkill(10)).thenReturn(shapeA);
        when(skillManager.getSkill(11)).thenReturn(shapeB);
        when(skillManager.getSkill(5001)).thenReturn(modifier);
        when(skillManager.getModifier(anyInt())).thenReturn(modifier);
        when(skillManager.getShiftedPartID(shapeA)).thenReturn(10);
        when(skillManager.getShiftedPartID(shapeB)).thenReturn(11);
        when(skillManager.getShiftedPartID(modifier)).thenReturn(5001);

        SkillManager.instance = skillManager;
        ItemsCommonProxy.spell = mock(am2.items.SpellBase.class);
    }

    @After
    public void restoreRegistry() {
        SkillManager.instance = previousSkillManager;
        ItemsCommonProxy.spell = (am2.items.SpellBase) previousSpell;
    }

    @Test
    public void duplicateModifiersArePreservedInTheSelectedShapeGroup() {
        ItemStack input = inputStack();
        NBTTagCompound tag = input.getTagCompound();
        tag.setInteger("NumShapeGroups", 2);
        tag.setInteger("CurrentShapeGroup", 1);
        tag.setIntArray("ShapeGroup_0", new int[] { 10 });
        tag.setIntArray("ShapeGroup_1", new int[] { 11, 5001, 5001 });

        ItemStack constructed = SpellUtils.instance.constructSpellStack(input);

        assertNotSame(input, constructed);
        assertEquals(1, SpellUtils.instance.numStages(constructed));
        assertEquals(
            11,
            constructed.getTagCompound()
                .getInteger("ShapeOrdinal_0"));
        assertEquals(
            0,
            constructed.getTagCompound()
                .getIntArray("SpellComponentIDs_0").length);
        assertEquals(
            2,
            constructed.getTagCompound()
                .getIntArray("SpellModifierIDs_0").length);
    }

    @Test
    public void aSummonInALaterOldStageIsCopied() {
        ItemStack input = inputStack();
        NBTTagCompound tag = input.getTagCompound();
        tag.setInteger("NumShapeGroups", 1);
        tag.setInteger("CurrentShapeGroup", 0);
        tag.setIntArray("ShapeGroup_0", new int[] { 10 });
        tag.setInteger("NumStages", 2);
        tag.setInteger("ShapeOrdinal_0", 10);
        tag.setIntArray("SpellComponentIDs_0", new int[] { 100 });
        tag.setInteger("ShapeOrdinal_1", 11);
        tag.setIntArray("SpellComponentIDs_1", new int[] { 200 });

        ItemStack constructed = SpellUtils.instance.constructSpellStack(input);

        assertEquals(3, SpellUtils.instance.numStages(constructed));
        assertEquals(
            11,
            constructed.getTagCompound()
                .getInteger("ShapeOrdinal_2"));
        assertArrayEquals(
            new int[] { 200 },
            constructed.getTagCompound()
                .getIntArray("SpellComponentIDs_2"));
    }

    @Test
    public void anEmptyRecipeProducesAnInertSpellStack() {
        ItemStack constructed = SpellUtils.instance
            .createSpellStack(new java.util.ArrayList<>(), new java.util.ArrayList<>());

        assertNotNull(constructed);
        assertEquals(0, SpellUtils.instance.numStages(constructed));
    }

    @Test
    public void shapeGroupsAreOnlySelectedByTheCurrentIndex() {
        ItemStack input = inputStack();
        NBTTagCompound tag = input.getTagCompound();
        tag.setInteger("NumShapeGroups", 2);
        tag.setInteger("CurrentShapeGroup", 0);
        tag.setIntArray("ShapeGroup_0", new int[] { 10 });
        tag.setIntArray("ShapeGroup_1", new int[] { 11 });

        ItemStack constructed = SpellUtils.instance.constructSpellStack(input);

        assertEquals(1, SpellUtils.instance.numStages(constructed));
        assertEquals(
            10,
            constructed.getTagCompound()
                .getInteger("ShapeOrdinal_0"));
    }

    private static ItemStack inputStack() {
        ItemStack stack = new ItemStack(mock(Item.class));
        stack.setTagCompound(new NBTTagCompound());
        return stack;
    }
}
