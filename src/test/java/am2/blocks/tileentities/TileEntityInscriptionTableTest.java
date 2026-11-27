package am2.blocks.tileentities;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

import am2.api.spell.component.interfaces.ISpellPart;

/**
 * Pins the bounded multi-removal helpers. The old loops used {@code i <= length}, removing one element too many and
 * throwing when the selection ended at the list end.
 */
public class TileEntityInscriptionTableTest {

    private TileEntityInscriptionTable table;

    @Before
    public void setUp() {
        table = new TileEntityInscriptionTable();
        World world = mock(World.class);
        world.isRemote = false;
        table.setWorldObj(world);
    }

    @Test
    public void stageGroupRemovalRemovesExactlyTheRequestedCount() {
        ISpellPart a = mock(ISpellPart.class);
        ISpellPart b = mock(ISpellPart.class);
        ISpellPart c = mock(ISpellPart.class);
        table.addSpellPartToStageGroup(0, a);
        table.addSpellPartToStageGroup(0, b);
        table.addSpellPartToStageGroup(0, c);
        assertEquals(3, table.getShapeGroupSize(0));

        table.removeMultipleSpellPartsFromStageGroup(0, 2, 0);
        assertEquals(1, table.getShapeGroupSize(0));
        assertEquals(c, table.getShapeGroupPartAt(0, 0));
    }

    @Test
    public void stageGroupRemovalAtTheEndDoesNotThrow() {
        table.addSpellPartToStageGroup(0, mock(ISpellPart.class));
        table.addSpellPartToStageGroup(0, mock(ISpellPart.class));

        table.removeMultipleSpellPartsFromStageGroup(0, 10, 0);
        assertEquals(0, table.getShapeGroupSize(0));
    }

    @Test
    public void invalidStageGroupArgumentsAreIgnored() {
        table.addSpellPartToStageGroup(0, mock(ISpellPart.class));
        table.removeMultipleSpellPartsFromStageGroup(0, 1, -1);
        table.removeMultipleSpellPartsFromStageGroup(0, 1, 99);
        table.removeMultipleSpellPartsFromStageGroup(5, 1, 0);
        assertEquals(1, table.getShapeGroupSize(0));
    }

    @Test
    public void recipeRemovalRemovesExactlyTheRequestedCount() {
        ISpellPart a = mock(ISpellPart.class);
        ISpellPart b = mock(ISpellPart.class);
        ISpellPart c = mock(ISpellPart.class);
        table.addSpellPart(a);
        table.addSpellPart(b);
        table.addSpellPart(c);
        assertEquals(
            3,
            table.getCurrentRecipe()
                .size());

        table.removeMultipleSpellParts(0, 2);
        assertEquals(
            1,
            table.getCurrentRecipe()
                .size());
        assertEquals(
            c,
            table.getCurrentRecipe()
                .get(0));
    }

    @Test
    public void recipeRemovalAtTheEndDoesNotThrow() {
        table.addSpellPart(mock(ISpellPart.class));
        table.removeMultipleSpellParts(0, 10);
        assertEquals(
            0,
            table.getCurrentRecipe()
                .size());
    }
}
