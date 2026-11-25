package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.affinity.AffinityHelper;
import am2.items.ItemsCommonProxy;

public class BlockOverlayHandlerTest {

    private Item previousHood;

    @Before
    public void replaceHood() {
        previousHood = ItemsCommonProxy.archmageHood;
        ItemsCommonProxy.archmageHood = mock(Item.class);
    }

    @After
    public void restoreHood() {
        ItemsCommonProxy.archmageHood = previousHood;
    }

    @Test
    public void normalWaterOverlayIsLeftAlone() {
        EntityPlayer player = mock(EntityPlayer.class);
        RenderBlockOverlayEvent event = new RenderBlockOverlayEvent(
            player,
            0.0F,
            OverlayType.WATER,
            mock(Block.class),
            0,
            0,
            0);
        AffinityHelper.isNotInWaterActually.remove(player);

        new BlockOverlayHandler().onBlockOverlay(event);

        assertFalse(event.isCanceled());
    }

    @Test
    public void fireOverlayRemainsWithoutTheHood() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        RenderBlockOverlayEvent event = new RenderBlockOverlayEvent(
            player,
            0.0F,
            OverlayType.FIRE,
            mock(Block.class),
            0,
            0,
            0);

        new BlockOverlayHandler().onBlockOverlay(event);

        assertFalse(event.isCanceled());
    }
}
