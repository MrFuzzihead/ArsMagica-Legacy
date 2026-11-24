package am2.entities.renderers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Covers the client player-model map that replaced the old coremod state.
 */
public class ClientPlayerModelMapTest {

    @Before
    public void resetBefore() {
        ClientPlayerModelMap.clear();
    }

    @After
    public void resetAfter() {
        ClientPlayerModelMap.clear();
    }

    @Test
    public void maidModelsAreSpecial() {
        EntityPlayer player = player("Alice");
        ClientPlayerModelMap.map()
            .put("Alice", "maid3");

        assertTrue(ClientPlayerModelMap.isSpecialModel(player));
    }

    @Test
    public void mobDisguisesAreNotSpecial() {
        EntityPlayer player = player("Bob");
        ClientPlayerModelMap.map()
            .put("Bob", "zombie");

        assertFalse(ClientPlayerModelMap.isSpecialModel(player));
    }

    @Test
    public void unknownPlayersAreNotSpecial() {
        assertFalse(ClientPlayerModelMap.isSpecialModel(player("Carol")));
        assertFalse(ClientPlayerModelMap.isSpecialModel(null));
    }

    @Test
    public void clearRemovesAllEntries() {
        ClientPlayerModelMap.map()
            .put("Dave", "maid0");
        assertEquals(
            1,
            ClientPlayerModelMap.map()
                .size());

        ClientPlayerModelMap.clear();
        assertEquals(
            0,
            ClientPlayerModelMap.map()
                .size());
    }

    private static EntityPlayer player(String name) {
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getCommandSenderName()).thenReturn(name);
        return player;
    }
}
