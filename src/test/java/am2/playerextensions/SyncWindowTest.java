package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * Pins the countdown semantics shared by ExtendedProperties, AffinityData and SkillData.
 */
public class SyncWindowTest {

    @Test
    public void nonPlayersAreIgnoredUnlessForced() {
        SyncWindow window = new SyncWindow(10);
        window.setTicks(5);

        assertFalse(window.shouldSend(false, true));
        assertEquals(5, window.ticks());

        window.force();
        assertTrue(window.shouldSend(false, true));
        assertEquals(10, window.ticks());
    }

    @Test
    public void playerSyncWaitsForTheDelayAndData() {
        SyncWindow window = new SyncWindow(10);
        window.setTicks(1);

        assertTrue(window.shouldSend(true, true));
        assertEquals(10, window.ticks());

        assertFalse(window.shouldSend(true, false));
        assertEquals(9, window.ticks());

        for (int i = 0; i < 8; i++) {
            assertFalse(window.shouldSend(true, true));
        }
        assertTrue(window.shouldSend(true, true));
    }

    @Test
    public void fullSyncAndForceResetTheCountdown() {
        SyncWindow window = new SyncWindow(10);
        window.setTicks(8);
        window.fullSync();
        assertEquals(0, window.ticks());
        assertTrue(window.isForcing());

        window.clearForcing();
        assertFalse(window.isForcing());

        window.setTicks(8);
        window.force();
        assertEquals(0, window.ticks());
        assertTrue(window.isForcing());
    }

    @Test
    public void settingTicksDoesNotChangeForcingState() {
        SyncWindow window = new SyncWindow(10);
        window.force();
        window.setTicks(4);

        assertEquals(4, window.ticks());
        assertTrue(window.isForcing());
    }

    @Test
    public void randomInitialTicksStayInRange() {
        SyncWindow window = new SyncWindow(10);
        Random random = new Random(0);

        window.setTicks(random.nextInt(10));

        assertTrue(window.ticks() >= 0);
        assertTrue(window.ticks() < 10);
        assertFalse(window.isForcing());
    }
}
