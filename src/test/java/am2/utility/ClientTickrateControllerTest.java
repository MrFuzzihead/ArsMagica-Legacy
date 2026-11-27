package am2.utility;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the client timer state that replaced the reflective {@code Minecraft.timer} update.
 */
public class ClientTickrateControllerTest {

    @Before
    public void resetBefore() {
        ClientTickrateController.resetForTesting();
    }

    @After
    public void resetAfter() {
        ClientTickrateController.resetForTesting();
    }

    @Test
    public void defaultClientTickrateIsVanilla() {
        assertEquals(TickrateController.DEFAULT_TICKS_PER_SECOND, ClientTickrateController.getClientTickrate(), 0.0F);
    }
}
