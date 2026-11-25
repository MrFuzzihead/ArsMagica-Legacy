package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.playerextensions.effects.TimedBlockEffect;

/**
 * The accelerated-tile duration is keyed by a string such as {@code accelerated_fast_tile_x_y_z_dim_power}. Parsing
 * that string every tick was unnecessary; the bounded cache keeps the parsed coordinates for the effect's lifetime.
 */
public class AMEventHandlerAcceleratedTileKeyTest {

    @Before
    public void clearCache() {
        AMEventHandler.clearAcceleratedTileKeyCache();
    }

    @After
    public void clearCacheAfter() {
        AMEventHandler.clearAcceleratedTileKeyCache();
    }

    @Test
    public void parsesAllCoordinatesAndPower() {
        TimedBlockEffect key = AMEventHandler.acceleratedTileKey("accelerated_fast_tile_10_64_-3_0_4");

        assertEquals(10, key.x);
        assertEquals(64, key.y);
        assertEquals(-3, key.z);
        assertEquals(0, key.dimension);
        assertEquals(4, key.power);
    }

    @Test
    public void malformedKeysAreRejectedAndNotCached() {
        assertNull(AMEventHandler.acceleratedTileKey(null));
        assertNull(AMEventHandler.acceleratedTileKey("accelerated_fast_tile_1_2"));
        assertNull(AMEventHandler.acceleratedTileKey("accelerated_fast_tile_a_b_c_d_e"));
        assertEquals(0, AMEventHandler.acceleratedTileKeyCacheSize());
    }

    @Test
    public void repeatedLookupsReuseTheParsedKey() {
        String raw = "accelerated_fast_tile_1_2_3_4_5";

        TimedBlockEffect first = AMEventHandler.acceleratedTileKey(raw);
        TimedBlockEffect second = AMEventHandler.acceleratedTileKey(raw);

        assertSame(first, second);
        assertEquals(1, AMEventHandler.acceleratedTileKeyCacheSize());
    }

    @Test
    public void differentCoordinatesDoNotShareAKey() {
        assertNotSame(
            AMEventHandler.acceleratedTileKey("accelerated_fast_tile_1_2_3_4_5"),
            AMEventHandler.acceleratedTileKey("accelerated_fast_tile_1_2_3_4_6"));
        assertEquals(2, AMEventHandler.acceleratedTileKeyCacheSize());
    }

    @Test
    public void cacheIsBounded() {
        for (int i = 0; i < AMEventHandler.MAX_ACCELERATED_TILE_KEYS + 32; i++) {
            AMEventHandler.acceleratedTileKey("accelerated_fast_tile_" + i + "_2_3_4_5");
        }

        assertTrue(AMEventHandler.acceleratedTileKeyCacheSize() <= AMEventHandler.MAX_ACCELERATED_TILE_KEYS);
    }
}
