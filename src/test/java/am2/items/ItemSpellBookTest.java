package am2.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import am2.api.spell.ItemSpellBase;

/**
 * Pins the spell-book NBT bounds: a crafted or legacy active-slot value must clamp instead of indexing a 40-element
 * array, and a non-spell item in the book must not be cast to {@code ItemSpellBase}.
 */
public class ItemSpellBookTest {

    @Test
    public void activeSlotBelowZeroClampsToZero() {
        ItemSpellBook book = new ItemSpellBook();
        ItemStack stack = new ItemStack(book);
        stack.setTagCompound(new NBTTagCompound());
        stack.stackTagCompound.setInteger("spellbookactiveslot", -1);

        assertEquals(0, book.GetActiveSlot(stack));
        assertEquals(0, stack.stackTagCompound.getInteger("spellbookactiveslot"));
    }

    @Test
    public void activeSlotAboveTheBookSizeClampsToZero() {
        ItemSpellBook book = new ItemSpellBook();
        ItemStack stack = new ItemStack(book);
        stack.setTagCompound(new NBTTagCompound());
        stack.stackTagCompound.setInteger("spellbookactiveslot", 100);

        assertEquals(0, book.GetActiveSlot(stack));
        assertEquals(0, stack.stackTagCompound.getInteger("spellbookactiveslot"));
    }

    @Test
    public void aNonSpellItemIsNotCastToASpell() {
        ItemStack plain = mock(ItemStack.class);
        when(plain.getItem()).thenReturn(new Item());

        assertNull(ItemSpellBook.activeSpell(plain));
        assertNull(ItemSpellBook.activeSpell(null));
    }

    @Test
    public void aSpellItemIsReturned() {
        ItemSpellBase spell = mock(ItemSpellBase.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(spell);

        assertSame(spell, ItemSpellBook.activeSpell(stack));
    }
}
