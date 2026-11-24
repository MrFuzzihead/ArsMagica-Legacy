package am2.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.Test;

import am2.blocks.tileentities.TileEntityInscriptionTable;
import am2.containers.ContainerInscriptionTable;

/**
 * Covers the server-side authority helpers used by the client-packet handlers.
 */
public class PacketAuthorityTest {

    @Test
    public void reachableTileEntityInTheSameWorldPasses() {
        World world = mock(World.class);
        EntityPlayer player = playerIn(world, 16.0D);
        TileEntity te = tileEntityIn(world);

        assertTrue(PacketAuthority.isWithinReach(player, te));
    }

    @Test
    public void distantTileEntityFails() {
        World world = mock(World.class);
        EntityPlayer player = playerIn(world, 64.01D);
        TileEntity te = tileEntityIn(world);

        assertFalse(PacketAuthority.isWithinReach(player, te));
    }

    @Test
    public void differentWorldFails() {
        EntityPlayer player = playerIn(mock(World.class), 0.0D);
        TileEntity te = tileEntityIn(mock(World.class));

        assertFalse(PacketAuthority.isWithinReach(player, te));
    }

    @Test
    public void nullArgumentsFail() {
        assertFalse(PacketAuthority.isWithinReach(null, mock(TileEntity.class)));
        assertFalse(PacketAuthority.isWithinReach(mock(EntityPlayer.class), null));
    }

    @Test
    public void inscriptionTableRequiresItsOwnOpenContainer() {
        World world = mock(World.class);
        EntityPlayer player = playerIn(world, 1.0D);
        TileEntityInscriptionTable table = mock(TileEntityInscriptionTable.class);
        when(table.getWorldObj()).thenReturn(world);

        ContainerInscriptionTable container = mock(ContainerInscriptionTable.class);
        when(container.getTable()).thenReturn(table);
        player.openContainer = container;

        assertTrue(PacketAuthority.isUsingInscriptionTable(player, table));
    }

    @Test
    public void anotherTableOrAContainerTypeFails() {
        World world = mock(World.class);
        EntityPlayer player = playerIn(world, 1.0D);
        TileEntityInscriptionTable table = mock(TileEntityInscriptionTable.class);
        when(table.getWorldObj()).thenReturn(world);

        ContainerInscriptionTable container = mock(ContainerInscriptionTable.class);
        when(container.getTable()).thenReturn(mock(TileEntityInscriptionTable.class));
        player.openContainer = container;
        assertFalse(PacketAuthority.isUsingInscriptionTable(player, table));

        player.openContainer = mock(Container.class);
        assertFalse(PacketAuthority.isUsingInscriptionTable(player, table));
    }

    private static EntityPlayer playerIn(World world, double distanceSq) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.worldObj = world;
        when(player.getDistanceSq(0.5D, 0.5D, 0.5D)).thenReturn(distanceSq);
        return player;
    }

    private static TileEntity tileEntityIn(World world) {
        TileEntity te = mock(TileEntity.class);
        when(te.getWorldObj()).thenReturn(world);
        return te;
    }
}
