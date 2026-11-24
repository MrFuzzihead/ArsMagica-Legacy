package am2.configuration;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pins the potion-ID cap enforced by the Mixin migration.
 *
 * <p>
 * Potion IDs are written as a signed byte to NBT and to the effect packets, so IDs outside {@code 1..127} silently
 * truncate. {@link AMConfig#validatePotionId(int, int)} keeps configured values in range and falls back to the first
 * free slot otherwise.
 * </p>
 */
public class AMConfigPotionIdTest {

    @Test
    public void idsInsideTheByteSafeRangeAreKept() {
        assertEquals(1, AMConfig.validatePotionId(1, 24));
        assertEquals(24, AMConfig.validatePotionId(24, 32));
        assertEquals(127, AMConfig.validatePotionId(127, 32));
    }

    @Test
    public void zeroAndNegativeIdsUseTheFallback() {
        assertEquals(24, AMConfig.validatePotionId(0, 24));
        assertEquals(24, AMConfig.validatePotionId(-1, 24));
        assertEquals(24, AMConfig.validatePotionId(-100, 24));
    }

    @Test
    public void idsAtOrAboveTheArraySizeUseTheFallback() {
        assertEquals(24, AMConfig.validatePotionId(128, 24));
        assertEquals(24, AMConfig.validatePotionId(255, 24));
        assertEquals(24, AMConfig.validatePotionId(Integer.MAX_VALUE, 24));
    }

    @Test
    public void anInvalidFallbackClampsToOne() {
        assertEquals(1, AMConfig.validatePotionId(0, 0));
        assertEquals(1, AMConfig.validatePotionId(0, 200));
    }
}
