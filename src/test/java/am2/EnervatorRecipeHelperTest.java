package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The enervator registry must not hand out its stored output; callers set stackSize and would otherwise corrupt it.
 */
public class EnervatorRecipeHelperTest {

    @Before
    @After
    public void clearRecipes() throws Exception {
        Field field = EnervatorRecipeHelper.class.getDeclaredField("recipes");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(EnervatorRecipeHelper.instance)).clear();
    }

    @Test
    public void registrationCopiesTheCallersOutput() {
        Item item = mock(Item.class);
        ItemStack input = input(item);
        ItemStack callerOutput = mock(ItemStack.class);
        callerOutput.stackSize = 64;
        ItemStack storedOutput = mock(ItemStack.class);
        when(callerOutput.copy()).thenReturn(storedOutput);

        EnervatorRecipeHelper.instance.registerRecipe(input, callerOutput);

        assertSame(
            storedOutput,
            EnervatorRecipeHelper.instance.getRecipes()
                .values()
                .iterator()
                .next());
        assertEquals(64, callerOutput.stackSize);
    }

    @Test
    public void getRecipeReturnsIndependentCopies() {
        Item item = mock(Item.class);
        ItemStack input = input(item);
        ItemStack callerOutput = mock(ItemStack.class);
        ItemStack storedOutput = mock(ItemStack.class);
        ItemStack firstServed = mock(ItemStack.class);
        ItemStack secondServed = mock(ItemStack.class);
        when(callerOutput.copy()).thenReturn(storedOutput);
        when(storedOutput.copy()).thenReturn(firstServed, secondServed);

        EnervatorRecipeHelper.instance.registerRecipe(input, callerOutput);

        ItemStack first = EnervatorRecipeHelper.instance.getRecipe(input);
        ItemStack second = EnervatorRecipeHelper.instance.getRecipe(input);

        assertSame(firstServed, first);
        assertSame(secondServed, second);
        assertNotSame(first, second);
        assertEquals(1, first.stackSize);
        assertEquals(1, second.stackSize);
        assertEquals(0, storedOutput.stackSize);
    }

    private static ItemStack input(Item item) {
        ItemStack input = mock(ItemStack.class);
        when(input.getItem()).thenReturn(item);
        when(input.getItemDamage()).thenReturn(0);
        when(input.copy()).thenReturn(input);
        return input;
    }
}
