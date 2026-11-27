package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import org.junit.Test;

public class CompendiumPickupHandlerTest {

    @Test
    public void nullPlayersAreIgnored() {
        new CompendiumPickupHandler().onPlayerPickupItem(new EntityItemPickupEvent(null, mock(EntityItem.class)));
    }

    @Test
    public void remoteUnknownItemsDoNotTouchTheProxy() {
        EntityPlayer player = mock(EntityPlayer.class);
        World world = mock(World.class);
        world.isRemote = true;
        player.worldObj = world;
        EntityItem entityItem = mock(EntityItem.class);
        ItemStack stack = mock(ItemStack.class);
        Item item = mock(Item.class);
        when(entityItem.getEntityItem()).thenReturn(stack);
        when(stack.getItem()).thenReturn(item);
        when(item.getUnlocalizedName()).thenReturn(null);

        new CompendiumPickupHandler().onPlayerPickupItem(new EntityItemPickupEvent(player, entityItem));
    }
}
