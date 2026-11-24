package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

import am2.api.spell.enums.SkillPointTypes;
import am2.network.AMDataWriter;

/**
 * Pins the packet-direction split: a client may ask to learn a validated skill, but it may not push a full
 * {@code CLIENT_SYNC} payload that sets its own known parts and spell points.
 */
public class SkillDataTest {

    @Test
    public void clientSyncIsRejectedFromAClient() {
        SkillData data = dataFor(42);

        assertFalse(data.handlePacketDataFromClient(clientSyncPacket(42, 7, 8, 9)));

        assertEquals(3, data.getSpellPoints(SkillPointTypes.BLUE));
        assertEquals(0, data.getSpellPoints(SkillPointTypes.GREEN));
        assertEquals(0, data.getSpellPoints(SkillPointTypes.RED));
    }

    @Test
    public void clientSyncIsStillAcceptedOnTheServerToClientPath() {
        SkillData data = dataFor(42);

        assertTrue(data.handlePacketData(clientSyncPacket(42, 7, 8, 9)));

        assertEquals(7, data.getSpellPoints(SkillPointTypes.BLUE));
        assertEquals(8, data.getSpellPoints(SkillPointTypes.GREEN));
        assertEquals(9, data.getSpellPoints(SkillPointTypes.RED));
    }

    @Test
    public void packetsForAnotherEntityAreRejected() {
        SkillData data = dataFor(42);
        assertFalse(data.handlePacketDataFromClient(clientSyncPacket(7, 1, 2, 3)));
    }

    private static SkillData dataFor(int entityId) {
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getEntityId()).thenReturn(entityId);
        return new SkillData(player);
    }

    private static byte[] clientSyncPacket(int entityId, int blue, int green, int red) {
        AMDataWriter writer = new AMDataWriter();
        writer.add(SkillData.CLIENT_SYNC);
        writer.add(entityId);
        writer.add(0x8); // NUM_SPELL_POINTS
        writer.add(false); // no primary tree change
        writer.add(blue);
        writer.add(green);
        writer.add(red);
        return writer.generate();
    }
}
