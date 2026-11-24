package am2.spell.components;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Test;

import am2.AMCore;

/**
 * Pins the optional CoFH guard: without CoFH the RF-drain component must fail cleanly before resolving its classes.
 */
public class RedstoneFluxDrainTest {

    private final boolean savedCofh = AMCore.cofh;

    @After
    public void restoreCofhFlag() {
        AMCore.cofh = savedCofh;
    }

    @Test
    public void blockEffectFailsWithoutCofh() {
        AMCore.cofh = false;
        World world = mock(World.class);
        RedstoneFluxDrain drain = new RedstoneFluxDrain();

        assertFalse(drain.applyEffectBlock(null, world, 1, 2, 3, 0, 0, 0, 0, mock(EntityPlayer.class)));

        verify(world, never()).getTileEntity(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void entityEffectFailsWithoutCofh() {
        AMCore.cofh = false;
        RedstoneFluxDrain drain = new RedstoneFluxDrain();

        assertFalse(
            drain.applyEffectEntity(null, mock(World.class), mock(EntityPlayer.class), mock(EntityPlayer.class)));
    }
}
