package am2.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pins the dimension and biome ID sanitisation used before registering the MMF dimension and AM2 biomes.
 */
public class AMConfigIdTest {

    @Test
    public void reservedDimensionsFallBack() {
        assertEquals(-31, AMConfig.sanitizeDimensionId(0, -31, id -> false));
        assertEquals(-31, AMConfig.sanitizeDimensionId(-1, -31, id -> false));
        assertEquals(-31, AMConfig.sanitizeDimensionId(1, -31, id -> false));
    }

    @Test
    public void alreadyRegisteredDimensionsFallBack() {
        assertEquals(-31, AMConfig.sanitizeDimensionId(7, -31, id -> id == 7));
    }

    @Test
    public void freeDimensionsAreKept() {
        assertEquals(7, AMConfig.sanitizeDimensionId(7, -31, id -> false));
        assertEquals(-31, AMConfig.sanitizeDimensionId(-31, -31, id -> false));
    }

    @Test
    public void freeBiomeIdsAreKept() {
        assertEquals(100, AMConfig.sanitizeBiomeId(100, 110, id -> false));
    }

    @Test
    public void occupiedOrInvalidBiomeIdsFallBack() {
        assertEquals(110, AMConfig.sanitizeBiomeId(100, 110, id -> id == 100));
        assertEquals(110, AMConfig.sanitizeBiomeId(0, 110, id -> false));
        assertEquals(110, AMConfig.sanitizeBiomeId(300, 110, id -> false));
    }

    @Test
    public void bothOccupiedScansForAnotherSlot() {
        assertEquals(40, AMConfig.sanitizeBiomeId(100, 110, id -> id == 100 || id == 110));
    }

    @Test
    public void aFullBiomeListReturnsMinusOne() {
        assertEquals(-1, AMConfig.sanitizeBiomeId(100, 110, id -> id >= 1 && id <= 255));
    }
}
