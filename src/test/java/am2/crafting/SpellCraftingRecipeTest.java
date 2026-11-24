package am2.crafting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Before;
import org.junit.Test;

import am2.armor.BoundArmor;
import am2.items.ItemSpellStaff;
import am2.items.SpellBase;

/**
 * Covers the classification logic of the dynamic staff/spell/armor recipe without touching the crafting table.
 */
public class SpellCraftingRecipeTest {

    private InventoryCrafting grid;

    @Before
    public void setUp() {
        grid = mock(InventoryCrafting.class);
        when(grid.getStackInRowAndColumn(anyInt(), anyInt())).thenReturn(null);
    }

    @Test
    public void staffAndScrollSelectsTheScrollRecipe() {
        ItemStack staff = staff(false);
        ItemStack scroll = stackOf(mock(SpellBase.class));
        put(0, 0, staff);
        put(1, 1, scroll);

        SpellCraftingRecipe.Scan scan = SpellCraftingRecipe.scan(grid);
        assertTrue(SpellCraftingRecipe.INSTANCE.matches(grid, null));
        assertEquals("STAFF_SCROLL", scan.type.name());
        assertSame(staff, scan.firstStaff);
        assertSame(scroll, scan.spell);
    }

    @Test
    public void twoStaffsSelectTheChargeRecipeInColumnMajorOrder() {
        ItemStack first = staff(false);
        ItemStack second = staff(false);
        put(0, 0, first);
        put(0, 1, second);

        SpellCraftingRecipe.Scan scan = SpellCraftingRecipe.scan(grid);
        assertTrue(SpellCraftingRecipe.INSTANCE.matches(grid, null));
        assertEquals("STAFF_CHARGE", scan.type.name());
        assertSame(first, scan.firstStaff);
        assertSame(second, scan.secondStaff);
    }

    @Test
    public void armorAndScrollSelectsTheArmorRecipe() {
        ItemStack armor = stackOf(mock(BoundArmor.class));
        ItemStack scroll = stackOf(mock(SpellBase.class));
        put(2, 2, armor);
        put(0, 0, scroll);

        SpellCraftingRecipe.Scan scan = SpellCraftingRecipe.scan(grid);
        assertTrue(SpellCraftingRecipe.INSTANCE.matches(grid, null));
        assertEquals("ARMOR_SCROLL", scan.type.name());
        assertSame(armor, scan.armor);
        assertSame(scroll, scan.spell);
    }

    @Test
    public void magitechStaffsDoNotParticipate() {
        put(0, 0, staff(true));
        put(1, 1, stackOf(mock(SpellBase.class)));

        assertEquals("NONE", SpellCraftingRecipe.scan(grid).type.name());
        assertFalse(SpellCraftingRecipe.INSTANCE.matches(grid, null));
    }

    @Test
    public void twoStaffsWinOverAnAdditionalScroll() {
        put(0, 0, staff(false));
        put(0, 1, staff(false));
        put(1, 1, stackOf(mock(SpellBase.class)));

        assertEquals("STAFF_CHARGE", SpellCraftingRecipe.scan(grid).type.name());
    }

    @Test
    public void emptyGridDoesNotMatch() {
        assertEquals("NONE", SpellCraftingRecipe.scan(grid).type.name());
        assertFalse(SpellCraftingRecipe.INSTANCE.matches(grid, null));
    }

    private void put(int column, int row, ItemStack stack) {
        when(grid.getStackInRowAndColumn(column, row)).thenReturn(stack);
    }

    private static ItemStack staff(boolean magitech) {
        ItemSpellStaff item = mock(ItemSpellStaff.class);
        when(item.isMagiTechStaff()).thenReturn(magitech);
        return stackOf(item);
    }

    private static ItemStack stackOf(Item item) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.copy()).thenReturn(stack);
        return stack;
    }
}
