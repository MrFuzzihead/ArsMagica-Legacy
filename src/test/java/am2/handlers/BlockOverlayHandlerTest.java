package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
    public void waterOverlayIsSuppressedForWaterWalkingPlayers() {
        EntityPlayer player = mock(EntityPlayer.class);
        AffinityHelper.isNotInWaterActually.add(player);
        try {
            RenderBlockOverlayEvent event = cancelable(player, OverlayType.WATER);

            new BlockOverlayHandler().onBlockOverlay(event);

            assertTrue(event.isCanceled());
        } finally {
            AffinityHelper.isNotInWaterActually.remove(player);
        }
    }

    @Test
    public void fireOverlayIsSuppressedByTheArchmageHood() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        ItemStack hood = mock(ItemStack.class);
        when(hood.getItem()).thenReturn(ItemsCommonProxy.archmageHood);
        player.inventory.armorInventory[3] = hood;
        RenderBlockOverlayEvent event = cancelable(player, OverlayType.FIRE);

        new BlockOverlayHandler().onBlockOverlay(event);

        assertTrue(event.isCanceled());
    }

    @Test
    public void normalWaterOverlayIsLeftAlone() {
        EntityPlayer player = mock(EntityPlayer.class);
        AffinityHelper.isNotInWaterActually.remove(player);

        RenderBlockOverlayEvent event = cancelable(player, OverlayType.WATER);
        new BlockOverlayHandler().onBlockOverlay(event);

        assertFalse(event.isCanceled());
    }

    @Test
    public void fireOverlayRemainsWithoutTheHood() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);

        RenderBlockOverlayEvent event = cancelable(player, OverlayType.FIRE);
        new BlockOverlayHandler().onBlockOverlay(event);

        assertFalse(event.isCanceled());
    }

    private static RenderBlockOverlayEvent cancelable(EntityPlayer player, OverlayType type) {
        return new RenderBlockOverlayEvent(player, 0.0F, type, mock(Block.class), 0, 0, 0) {

            @Override
            public boolean isCancelable() {
                return true;
            }
        };
    }
}
