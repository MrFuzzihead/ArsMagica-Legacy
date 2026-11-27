package am2.handlers;

import static org.mockito.Mockito.mock;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import org.junit.Test;

public class ItemWatchHandlerTest {

    @Test
    public void fakePlayerInteractionsDoNotStartWatching() {
        FakePlayer player = mock(FakePlayer.class);
        EntityInteractEvent event = new EntityInteractEvent(player, mock(EntityItemFrame.class));

        new ItemWatchHandler().onEntityInteract(event);
    }

    @Test
    public void nonFrameTargetsDoNotStartWatching() {
        EntityPlayer player = mock(EntityPlayer.class);
        EntityInteractEvent event = new EntityInteractEvent(player, mock(Entity.class));

        new ItemWatchHandler().onEntityInteract(event);
    }

    @Test
    public void remoteItemTossesAreNotWatched() {
        EntityItem item = mock(EntityItem.class);
        World world = mock(World.class);
        world.isRemote = true;
        item.worldObj = world;
        ItemTossEvent event = new ItemTossEvent(item, mock(EntityPlayer.class));

        new ItemWatchHandler().onPlayerTossItem(event);
    }
}
