package am2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The timed-effect bookkeeping in {@code onEntityLiving} mutates server worlds and reads server dimensions. These
 * guards keep it off the client and out of a null-world path.
 */
public class AMEventHandlerPerfGuardTest {

    private boolean accelerate;
    private boolean slow;
    private boolean vortex;
    private boolean fortify;
    private boolean shield;

    @Before
    public void saveFlags() {
        accelerate = AMEventHandler.enabled_accelerate;
        slow = AMEventHandler.enabled_slow;
        vortex = AMEventHandler.enable_spatialVortex;
        fortify = AMEventHandler.enabled_timeFortified;
        shield = AMEventHandler.enabled_shield;
    }

    @After
    public void restoreFlags() {
        AMEventHandler.enabled_accelerate = accelerate;
        AMEventHandler.enabled_slow = slow;
        AMEventHandler.enable_spatialVortex = vortex;
        AMEventHandler.enabled_timeFortified = fortify;
        AMEventHandler.enabled_shield = shield;
    }

    @Test
    public void remoteWorldsNeverRunTimedBookkeeping() {
        setFlags(true, true, true, true, true);
        World clientWorld = mock(World.class);
        clientWorld.isRemote = true;

        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(clientWorld));
        assertFalse(AMEventHandler.shouldTickTimedEffects(clientWorld));
    }

    @Test
    public void nullWorldsAreIgnored() {
        setFlags(true, true, true, true, true);

        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(null));
        assertFalse(AMEventHandler.shouldTickTimedEffects(null));
    }

    @Test
    public void serverWorldsRunOnlyEnabledEffects() {
        World serverWorld = mock(World.class);
        serverWorld.isRemote = false;

        setFlags(true, false, false, false, false);
        assertTrue(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, false, false);
        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertFalse(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, true, false, false, false);
        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, true, false, false);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, true, false);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, false, true);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));
    }

    private static void setFlags(boolean accelerate, boolean slow, boolean vortex, boolean fortify, boolean shield) {
        AMEventHandler.enabled_accelerate = accelerate;
        AMEventHandler.enabled_slow = slow;
        AMEventHandler.enable_spatialVortex = vortex;
        AMEventHandler.enabled_timeFortified = fortify;
        AMEventHandler.enabled_shield = shield;
    }
}
