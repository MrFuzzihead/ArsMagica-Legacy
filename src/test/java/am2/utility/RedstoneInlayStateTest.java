package am2.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

/**
 * Covers the Redstone Signal inlay counter: a matching coordinate supplies power once per tick and is removed when
 * its duration reaches zero.
 */
public class RedstoneInlayStateTest {

    private World world;
    private Block block;

    @Before
    public void setUp() {
        RedstoneInlayState.clear();

        world = mock(World.class);

        block = mock(Block.class);
        when(world.getBlock(1, 2, 3)).thenReturn(block);
    }

    @Test
    public void addedInlaySuppliesPowerAndDecrements() {
        RedstoneInlayState.add(0, 1, 2, 3, 2);
        assertEquals(1, RedstoneInlayState.size());

        assertTrue(RedstoneInlayState.isProvidingRedstone(0, world, 1, 2, 3));
        assertEquals(1, RedstoneInlayState.size());

        assertTrue(RedstoneInlayState.isProvidingRedstone(0, world, 1, 2, 3));
        assertEquals(0, RedstoneInlayState.size());

        assertFalse(RedstoneInlayState.isProvidingRedstone(0, world, 1, 2, 3));
        verify(block, times(2)).onNeighborBlockChange(eq(world), eq(1), eq(2), eq(3), nullable(Block.class));
    }

    @Test
    public void differentCoordinateDoesNotSupplyPower() {
        RedstoneInlayState.add(0, 1, 2, 3, 5);
        assertFalse(RedstoneInlayState.isProvidingRedstone(0, world, 9, 9, 9));
        assertEquals(1, RedstoneInlayState.size());
        verify(block, never()).onNeighborBlockChange(eq(world), eq(1), eq(2), eq(3), nullable(Block.class));
    }

    @Test
    public void differentDimensionDoesNotSupplyPower() {
        RedstoneInlayState.add(0, 1, 2, 3, 5);
        assertFalse(RedstoneInlayState.isProvidingRedstone(-1, world, 1, 2, 3));
        assertEquals(1, RedstoneInlayState.size());
    }

    @Test
    public void clearRemovesEverything() {
        RedstoneInlayState.add(0, 1, 2, 3, 5);
        RedstoneInlayState.add(0, 4, 5, 6, 5);
        assertEquals(2, RedstoneInlayState.size());
        RedstoneInlayState.clear();
        assertEquals(0, RedstoneInlayState.size());
    }
}
