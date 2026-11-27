package am2.blocks.tileentities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;

import org.junit.Test;

public class TileEntityOcculusTest {

    private final TileEntityOcculus occulus = mock(TileEntityOcculus.class, CALLS_REAL_METHODS);

    @Test
    public void onlyTheBookSlotAcceptsWritableBooks() {
        assertTrue(occulus.isItemValidForSlot(0, book()));
        assertFalse(occulus.isItemValidForSlot(1, book()));
    }

    @Test
    public void nullAndNonBookStacksAreRejected() {
        assertFalse(occulus.isItemValidForSlot(0, null));
        assertFalse(occulus.isItemValidForSlot(0, plainStack()));
    }

    private static ItemStack book() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(mock(ItemWritableBook.class));
        return stack;
    }

    private static ItemStack plainStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(mock(Item.class));
        return stack;
    }
}
