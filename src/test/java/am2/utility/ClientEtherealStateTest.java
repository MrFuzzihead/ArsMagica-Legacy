package am2.utility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the client ethereal-camera flag without launching Minecraft. The {@code isEtherealCamera} check short-circuits
 * on the flag, so the false path is safe headlessly.
 */
public class ClientEtherealStateTest {

    @Before
    public void resetBefore() {
        ClientEtherealState.setOrientingCamera(false);
    }

    @After
    public void resetAfter() {
        ClientEtherealState.setOrientingCamera(false);
    }

    @Test
    public void defaultStateIsInactive() {
        assertFalse(ClientEtherealState.isOrientingCamera());
        assertFalse(ClientEtherealState.isEtherealCamera());
    }

    @Test
    public void flagCanBeRaisedAndCleared() {
        ClientEtherealState.setOrientingCamera(true);
        assertTrue(ClientEtherealState.isOrientingCamera());

        ClientEtherealState.setOrientingCamera(false);
        assertFalse(ClientEtherealState.isOrientingCamera());
    }
}
