package am2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Test;

import cofh.api.energy.IEnergyHandler;

/**
 * Covers the null/optional-mod guards used by the spatial-vortex and Fortify-Time paths.
 */
public class AMEventHandlerSafetyTest {

    private final boolean savedCofh = AMCore.cofh;

    @After
    public void restoreCofhFlag() {
        AMCore.cofh = savedCofh;
    }

    @Test
    public void nullWorldYieldsNoTileEntity() {
        assertNull(AMEventHandler.getTileEntitySafe(null, 1, 2, 3));
    }

    @Test
    public void tileEntityLookupDelegates() {
        World world = mock(World.class);
        TileEntity te = mock(TileEntity.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(te);

        assertSame(te, AMEventHandler.getTileEntitySafe(world, 1, 2, 3));
    }

    @Test
    public void energyHandlersAreIgnoredWhenCofhIsAbsent() {
        AMCore.cofh = false;
        assertFalse(AMEventHandler.isEnergyHandler(mock(IEnergyHandler.class)));
    }

    @Test
    public void energyHandlersAreRecognisedWhenCofhIsPresent() {
        AMCore.cofh = true;
        assertTrue(AMEventHandler.isEnergyHandler(mock(IEnergyHandler.class)));
        assertFalse(AMEventHandler.isEnergyHandler(new Object()));
    }

    @Test
    public void missingWorldOrTileDoesNotThrowWhenReleasingAFortifiedChunk() {
        AMEventHandler.releaseFortifiedChunk(null, 1, 2, 3);

        World world = mock(World.class);
        when(world.getTileEntity(1, 2, 3)).thenReturn(null);
        AMEventHandler.releaseFortifiedChunk(world, 1, 2, 3);
    }
}
