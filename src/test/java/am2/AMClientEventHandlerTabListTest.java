package am2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.client.gui.GuiPlayerInfo;

import org.junit.Test;

public class AMClientEventHandlerTabListTest {

    @Test
    public void onlyLocalPlayerIsDetectedByNameOrDisplayName() {
        assertFalse(
            AMClientEventHandler.hasOtherPlayers(
                Arrays.asList(new GuiPlayerInfo("me"), new GuiPlayerInfo("My Name")),
                "My Name",
                "me"));
    }

    @Test
    public void anotherPlayerIsDetected() {
        assertTrue(
            AMClientEventHandler
                .hasOtherPlayers(Arrays.asList(new GuiPlayerInfo("me"), new GuiPlayerInfo("friend")), "me"));
    }

    @Test
    public void missingTabListIsTreatedAsNoOtherPlayers() {
        assertFalse(AMClientEventHandler.hasOtherPlayers(null, "me"));
        assertFalse(AMClientEventHandler.hasOtherPlayers(Collections.<GuiPlayerInfo>emptyList(), "me"));
    }
}
