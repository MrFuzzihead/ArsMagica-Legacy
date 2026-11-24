package am2.api.power;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Pins the cached power-type lookup contract used by power ticks, NBT loading and request loops.
 */
public class PowerTypesTest {

    @Test
    public void builtInTypesResolveByIdentity() {
        assertSame(PowerTypes.LIGHT, PowerTypes.getByID(1));
        assertSame(PowerTypes.NEUTRAL, PowerTypes.getByID(2));
        assertSame(PowerTypes.DARK, PowerTypes.getByID(4));
    }

    @Test
    public void unknownIdsAndNoneFallBackToNone() {
        assertSame(PowerTypes.NONE, PowerTypes.getByID(0));
        assertSame(PowerTypes.NONE, PowerTypes.getByID(3));
        assertSame(PowerTypes.NONE, PowerTypes.getByID(-1));
        assertSame(PowerTypes.NONE, PowerTypes.getByID(Integer.MAX_VALUE));
    }

    @Test
    public void allUsesTheSameCachedArray() {
        assertSame(PowerTypes.all(), PowerTypes.all());
    }

    @Test
    public void allStillExcludesNone() {
        for (PowerTypes type : PowerTypes.all()) {
            assertNotSame(PowerTypes.NONE, type);
        }
        assertTrue(
            Arrays.asList(PowerTypes.all())
                .contains(PowerTypes.LIGHT));
        assertTrue(
            Arrays.asList(PowerTypes.all())
                .contains(PowerTypes.NEUTRAL));
        assertTrue(
            Arrays.asList(PowerTypes.all())
                .contains(PowerTypes.DARK));
    }

    @Test
    public void registrationUpdatesBothTheArrayAndTheIdIndex() {
        int id = 128;
        PowerTypes before = PowerTypes.getByID(id);
        assertSame(PowerTypes.NONE, before);

        PowerTypes.RegisterPowerType(id, "Test Type", "§a");
        PowerTypes registered = PowerTypes.getByID(id);

        assertSame(registered, PowerTypes.getByID(id));
        assertTrue(
            Arrays.asList(PowerTypes.all())
                .contains(registered));
        assertEquals("Test Type", registered.name());

        // Registering the same id again must not replace or duplicate the existing entry.
        int size = PowerTypes.all().length;
        PowerTypes.RegisterPowerType(id, "Duplicate", "§c");
        assertSame(registered, PowerTypes.getByID(id));
        assertEquals(size, PowerTypes.all().length);
        assertArrayEquals(PowerTypes.all(), PowerTypes.all());
    }
}
