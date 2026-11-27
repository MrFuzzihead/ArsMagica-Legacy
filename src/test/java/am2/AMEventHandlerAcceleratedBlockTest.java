package am2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

/**
 * Covers the extracted accelerated-block tick: one tile lookup, one block lookup, and one random roll per tick.
 */
public class AMEventHandlerAcceleratedBlockTest {

    private static final int X = 1;
    private static final int Y = 2;
    private static final int Z = 3;

    private World world;
    private TileEntity tile;
    private Block block;
    private Random random;

    @Before
    public void setUp() {
        world = mock(World.class);
        tile = mock(TileEntity.class);
        block = mock(Block.class);
        random = mock(Random.class);
        world.rand = random;

        when(world.getTileEntity(X, Y, Z)).thenReturn(tile);
        when(world.getBlock(X, Y, Z)).thenReturn(block);
    }

    @Test
    public void tickableTileIsUpdatedOncePerCall() {
        when(tile.canUpdate()).thenReturn(true);
        when(block.getTickRandomly()).thenReturn(false);

        AMEventHandler.tickAcceleratedBlock(world, X, Y, Z);

        verify(world, times(1)).getTileEntity(X, Y, Z);
        verify(world, times(1)).getBlock(X, Y, Z);
        verify(tile, times(1)).updateEntity();
        verify(block, never()).updateTick(world, X, Y, Z, random);
    }

    @Test
    public void nonUpdatingTileIsNotUpdated() {
        when(tile.canUpdate()).thenReturn(false);
        when(block.getTickRandomly()).thenReturn(false);

        AMEventHandler.tickAcceleratedBlock(world, X, Y, Z);

        verify(tile, never()).updateEntity();
    }

    @Test
    public void randomBlockTicksOnlyWhenTheRollSucceeds() {
        when(block.getTickRandomly()).thenReturn(true);
        when(random.nextInt(100)).thenReturn(0);

        AMEventHandler.tickAcceleratedBlock(world, X, Y, Z);

        verify(block, times(1)).updateTick(world, X, Y, Z, random);
    }

    @Test
    public void randomBlockDoesNotTickWhenTheRollFails() {
        when(block.getTickRandomly()).thenReturn(true);
        when(random.nextInt(100)).thenReturn(1);

        AMEventHandler.tickAcceleratedBlock(world, X, Y, Z);

        verify(block, never()).updateTick(world, X, Y, Z, random);
    }

    @Test
    public void missingTileAndBlockAreSafe() {
        when(world.getTileEntity(X, Y, Z)).thenReturn(null);
        when(world.getBlock(X, Y, Z)).thenReturn(null);

        AMEventHandler.tickAcceleratedBlock(world, X, Y, Z);

        verify(tile, never()).updateEntity();
        verify(block, never()).updateTick(world, X, Y, Z, random);
    }
}
