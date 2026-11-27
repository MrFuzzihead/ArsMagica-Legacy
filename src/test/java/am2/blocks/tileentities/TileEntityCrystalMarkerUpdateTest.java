package am2.blocks.tileentities;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.junit.Test;

public class TileEntityCrystalMarkerUpdateTest {

    @Test
    public void cyclePriorityBuildsOneDescriptionPacketForAllNearbyPlayers() {
        TileEntityCrystalMarker marker = spy(new TileEntityCrystalMarker());
        World world = mock(World.class);
        marker.setWorldObj(world);
        marker.xCoord = 1;
        marker.yCoord = 2;
        marker.zCoord = 3;

        EntityPlayerMP first = player();
        EntityPlayerMP second = player();
        when(world.getEntitiesWithinAABB(eq(EntityPlayerMP.class), any(AxisAlignedBB.class)))
            .thenReturn(Arrays.asList(first, second));
        Packet packet = mock(Packet.class);
        doReturn(packet).when(marker)
            .getDescriptionPacket();

        marker.cyclePriority();

        verify(marker, times(1)).getDescriptionPacket();
        verify(first.playerNetServerHandler).sendPacket(packet);
        verify(second.playerNetServerHandler).sendPacket(packet);
    }

    private static EntityPlayerMP player() {
        EntityPlayerMP player = mock(EntityPlayerMP.class);
        player.playerNetServerHandler = mock(net.minecraft.network.NetHandlerPlayServer.class);
        return player;
    }
}
