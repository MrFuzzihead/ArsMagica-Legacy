package am2.blocks.tileentities;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

import am2.containers.ContainerInscriptionTable;
import am2.network.AMDataWriter;

/**
 * Pins the server-side authority rules for inscription-table update packets: the sender owns the table session, the
 * packet cannot select another player, and non-creative clients cannot create spells.
 */
public class TileEntityInscriptionTableAuthorityTest {

    private World world;
    private TileEntityInscriptionTable table;
    private EntityPlayer actor;

    @Before
    public void setUp() {
        world = mock(World.class);
        world.isRemote = false;

        table = spy(new TileEntityInscriptionTable());
        table.setWorldObj(world);
        when(world.getTileEntity(0, 0, 0)).thenReturn(table);

        actor = mock(EntityPlayer.class);
        actor.capabilities = new PlayerCapabilities();
        actor.openContainer = mock(ContainerInscriptionTable.class);
        when(((ContainerInscriptionTable) actor.openContainer).getTable()).thenReturn(table);
        when(actor.getDistanceSq(0.5D, 0.5D, 0.5D)).thenReturn(1.0D);

        table.setInUse(actor);
    }

    @Test
    public void fullUpdateUsesTheSenderAsTheTableOwner() {
        table.setInUse(null);

        table.HandleUpdatePacket(fullUpdatePacket(false, 999, "name", false), actor);

        assertSame(actor, table.getCurrentPlayerUsing());
    }

    @Test
    public void fullUpdateCanReleaseTheTableOwner() {
        table.HandleUpdatePacket(fullUpdatePacket(true, 999, "name", false), actor);
        assertNull(table.getCurrentPlayerUsing());
    }

    @Test
    public void nonCreativeClientsCannotCreateSpells() {
        actor.capabilities.isCreativeMode = false;

        table.HandleUpdatePacket(makeSpellPacket(12345), actor);

        verify(table, never()).createSpellForPlayer(any());
    }

    @Test
    public void aNonPlayerEntityIdIsIgnoredInsteadOfCast() {
        actor.capabilities.isCreativeMode = false;

        table.HandleUpdatePacket(makeSpellPacket(0), actor);

        verify(table, never()).createSpellForPlayer(any());
    }

    @Test
    public void resetNameWithAnUnrelatedContainerDoesNotThrow() {
        actor.openContainer = mock(Container.class);

        table.HandleUpdatePacket(resetNamePacket(12345), actor);
    }

    private static byte[] fullUpdatePacket(boolean notInUse, int clientEntityId, String name, boolean readOnly) {
        AMDataWriter writer = new AMDataWriter();
        writer.add(TileEntityInscriptionTable.FULL_UPDATE);
        writer.add(notInUse);
        writer.add(clientEntityId);
        writer.add(0); // recipe parts
        writer.add(0); // shape groups
        writer.add(name);
        writer.add(readOnly);
        return writer.generate();
    }

    private static byte[] makeSpellPacket(int clientEntityId) {
        AMDataWriter writer = new AMDataWriter();
        writer.add(TileEntityInscriptionTable.MAKE_SPELL);
        writer.add(clientEntityId);
        return writer.generate();
    }

    private static byte[] resetNamePacket(int clientEntityId) {
        AMDataWriter writer = new AMDataWriter();
        writer.add(TileEntityInscriptionTable.RESET_NAME);
        writer.add(clientEntityId);
        return writer.generate();
    }
}
