package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Pins the ticket key used by the chunk loader. The old key used a {@code compareTo} that returned -1 for every
 * unequal pair, so lookups only worked by accident and the map could not be replaced with a hash lookup.
 */
public class AMChunkLoaderTest {

    @Test
    public void sameLocationAndDimensionAreEqual() {
        AMChunkLoader.TicketIdentifier a = new AMChunkLoader.TicketIdentifier(1, 2, 3, 0);
        AMChunkLoader.TicketIdentifier b = new AMChunkLoader.TicketIdentifier(1, 2, 3, 0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentCoordinatesOrDimensionAreDifferent() {
        AMChunkLoader.TicketIdentifier base = new AMChunkLoader.TicketIdentifier(1, 2, 3, 0);

        assertNotEquals(base, new AMChunkLoader.TicketIdentifier(2, 2, 3, 0));
        assertNotEquals(base, new AMChunkLoader.TicketIdentifier(1, 3, 3, 0));
        assertNotEquals(base, new AMChunkLoader.TicketIdentifier(1, 2, 4, 0));
        assertNotEquals(base, new AMChunkLoader.TicketIdentifier(1, 2, 3, -1));
    }
}
