package am2;

import static org.junit.Assert.assertEquals;
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
 * Registering a fuel comparator must not zero the caller's stack; only the stored comparator is normalized.
 */
public class ObeliskFuelHelperTest {

    @Before
    @After
    public void clearFuels() throws Exception {
        Field field = ObeliskFuelHelper.class.getDeclaredField("validFuels");
        field.setAccessible(true);
        ((Map<?, ?>) field.get(ObeliskFuelHelper.instance)).clear();
    }

    @Test
    public void registrationCopiesAndNormalizesTheStoredComparator() {
        Item item = mock(Item.class);
        ItemStack caller = mock(ItemStack.class);
        caller.stackSize = 5;
        ItemStack stored = mock(ItemStack.class);
        when(caller.copy()).thenReturn(stored);
        when(stored.getItem()).thenReturn(item);
        when(stored.getItemDamage()).thenReturn(0);

        ObeliskFuelHelper.instance.registerFuelType(caller, 100);

        assertEquals(5, caller.stackSize);
        assertEquals(0, stored.stackSize);

        ItemStack query = mock(ItemStack.class);
        when(query.getItem()).thenReturn(item);
        when(query.getItemDamage()).thenReturn(0);
        assertEquals(100, ObeliskFuelHelper.instance.getFuelBurnTime(query));
    }

    @Test
    public void nullRegistrationIsIgnored() {
        ObeliskFuelHelper.instance.registerFuelType(null, 100);
    }
}
