package am2.api.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Pins the ManaStack collection contract: equality deliberately ignores the amount, so the hash must too. Exact
 * comparisons remain available through {@link ManaStack#isManaStackIdentical(ManaStack)}.
 */
public class ManaStackTest {

    @Test
    public void equalManaAndTagUseTheSameHashRegardlessOfAmount() {
        ManaStack small = new ManaStack(ManaRegistry.LIGHT, 5);
        ManaStack large = new ManaStack(ManaRegistry.LIGHT, 10);

        assertEquals(small, large);
        assertEquals(small.hashCode(), large.hashCode());
        assertFalse(small.isManaStackIdentical(large));
    }

    @Test
    public void differentManaTypesAreNotEqual() {
        ManaStack light = new ManaStack(ManaRegistry.LIGHT, 5);
        ManaStack dark = new ManaStack(ManaRegistry.DARK, 5);
        assertNotEquals(light, dark);
    }
}
