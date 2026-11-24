package am2.worldgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Test;

/**
 * Pins the bounds-safe worldgen rolls. A configured 0 (the natural "disabled" value) or an inverted ore range must
 * never call {@link Random#nextInt(int)}.
 */
public class WorldgenMathTest {

    @Test
    public void zeroAndNegativeChanceBoundsDoNotRoll() {
        Random random = mock(Random.class);
        assertFalse(WorldgenMath.chance(random, 0));
        assertFalse(WorldgenMath.chance(random, -5));
        verify(random, never()).nextInt(anyInt());
    }

    @Test
    public void positiveChanceRolls() {
        Random random = mock(Random.class);
        when(random.nextInt(5)).thenReturn(0);
        assertTrue(WorldgenMath.chance(random, 5));

        when(random.nextInt(5)).thenReturn(1);
        assertFalse(WorldgenMath.chance(random, 5));
    }

    @Test
    public void chanceBelowAvoidsInvalidBounds() {
        Random random = mock(Random.class);
        assertFalse(WorldgenMath.chanceBelow(random, 0, 7));
        assertFalse(WorldgenMath.chanceBelow(random, 5, 0));
        verify(random, never()).nextInt(anyInt());

        when(random.nextInt(10)).thenReturn(6);
        assertTrue(WorldgenMath.chanceBelow(random, 10, 7));
        when(random.nextInt(10)).thenReturn(7);
        assertFalse(WorldgenMath.chanceBelow(random, 10, 7));
    }

    @Test
    public void equalOrInvertedOreHeightsCollapseToMin() {
        Random random = mock(Random.class);
        assertEquals(20, WorldgenMath.oreHeight(random, 20, 20));
        assertEquals(20, WorldgenMath.oreHeight(random, 20, 10));
        verify(random, never()).nextInt(anyInt());
    }

    @Test
    public void validOreRangeUsesTheDelta() {
        Random random = mock(Random.class);
        when(random.nextInt(10)).thenReturn(4);
        assertEquals(24, WorldgenMath.oreHeight(random, 20, 30));
    }

    @Test
    public void meteorRollSkipsAZeroBound() {
        Random random = mock(Random.class);
        assertFalse(WorldgenMath.meteorRoll(random, 0, 0));
        assertFalse(WorldgenMath.meteorRoll(random, 18, 0));
        verify(random, never()).nextInt(anyInt());
    }

    @Test
    public void meteorRollUsesFrequencyAndMoonPhase() {
        Random random = mock(Random.class);
        when(random.nextInt(1)).thenReturn(0);
        assertTrue(WorldgenMath.meteorRoll(random, 19, 0));

        when(random.nextInt(1000)).thenReturn(0);
        assertTrue(WorldgenMath.meteorRoll(random, 0, 1));
    }
}
