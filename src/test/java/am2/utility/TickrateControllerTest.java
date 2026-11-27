package am2.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pins the tickrate conversion and the invalid-input guards used by the {@code MinecraftServerMixin}.
 */
public class TickrateControllerTest {

    @Before
    public void resetBefore() {
        TickrateController.resetForTesting();
    }

    @After
    public void resetAfter() {
        TickrateController.resetForTesting();
    }

    @Test
    public void defaultIsVanilla() {
        assertEquals(TickrateController.DEFAULT_TICK_LENGTH_MS, TickrateController.getServerTickrate());
        assertEquals(50L, TickrateController.getServerTickrate());
    }

    @Test
    public void twentyTicksPerSecondIsFiftyMilliseconds() {
        TickrateController.setServerTickrate(20.0F);
        assertEquals(50L, TickrateController.getServerTickrate());
    }

    @Test
    public void tenTicksPerSecondIsOneHundredMilliseconds() {
        TickrateController.setServerTickrate(10.0F);
        assertEquals(100L, TickrateController.getServerTickrate());
    }

    @Test
    public void fractionalTickrateUsesIntegerDivision() {
        TickrateController.setServerTickrate(15.0F);
        assertEquals(66L, TickrateController.getServerTickrate());
    }

    @Test
    public void zeroResetsToVanilla() {
        TickrateController.setServerTickrate(0.0F);
        assertEquals(TickrateController.DEFAULT_TICK_LENGTH_MS, TickrateController.getServerTickrate());
    }

    @Test
    public void negativeResetsToVanilla() {
        TickrateController.setServerTickrate(-20.0F);
        assertEquals(TickrateController.DEFAULT_TICK_LENGTH_MS, TickrateController.getServerTickrate());
    }

    @Test
    public void nanResetsToVanilla() {
        TickrateController.setServerTickrate(Float.NaN);
        assertEquals(TickrateController.DEFAULT_TICK_LENGTH_MS, TickrateController.getServerTickrate());
    }

    @Test
    public void infiniteResetsToVanilla() {
        TickrateController.setServerTickrate(Float.POSITIVE_INFINITY);
        assertEquals(TickrateController.DEFAULT_TICK_LENGTH_MS, TickrateController.getServerTickrate());
    }

    @Test
    public void extremelyHighTickrateClampsToOneMillisecond() {
        TickrateController.setServerTickrate(100000.0F);
        assertEquals(1L, TickrateController.getServerTickrate());
    }

    @Test
    public void mixinMarkerStartsFalseAndCanBeSet() {
        assertFalse(TickrateController.isMixinApplied());
        TickrateController.markMixinApplied();
        assertTrue(TickrateController.isMixinApplied());
    }
}
