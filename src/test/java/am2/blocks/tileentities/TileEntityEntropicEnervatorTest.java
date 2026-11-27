package am2.blocks.tileentities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the enervator's update throttle: a full NBT packet every tick becomes roughly one every ten ticks.
 */
public class TileEntityEntropicEnervatorTest {

    @Test
    public void updatesAreThrottled() {
        TileEntityEntropicEnervator enervator = new TileEntityEntropicEnervator();

        assertTrue(enervator.shouldSendUpdate());
        for (int i = 0; i < TileEntityEntropicEnervator.SYNC_INTERVAL; i++) {
            assertFalse(enervator.shouldSendUpdate());
        }
        assertTrue(enervator.shouldSendUpdate());
    }
}
