package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.junit.Test;

public class SoulboundItemsHandlerTest {

    @Test
    public void itemGoesIntoTheFirstEmptyMainSlot() {
        EntityPlayer player = mock(EntityPlayer.class);
        InventoryPlayer inventory = mock(InventoryPlayer.class);
        inventory.mainInventory = new ItemStack[1];
        player.inventory = inventory;
        ItemStack item = mock(ItemStack.class);
        when(item.copy()).thenReturn(item);

        assertTrue(SoulboundItemsHandler.addToPlayerInventory(player, item));
        assertSame(item, inventory.mainInventory[0]);
    }

    @Test
    public void fullInventoryRejectsTheItem() {
        EntityPlayer player = mock(EntityPlayer.class);
        InventoryPlayer inventory = mock(InventoryPlayer.class);
        inventory.mainInventory = new ItemStack[] { mock(ItemStack.class) };
        player.inventory = inventory;

        assertFalse(SoulboundItemsHandler.addToPlayerInventory(player, mock(ItemStack.class)));
    }

    @Test
    public void nullInputsAreRejected() {
        assertFalse(SoulboundItemsHandler.addToPlayerInventory(null, mock(ItemStack.class)));
        assertFalse(SoulboundItemsHandler.addToPlayerInventory(mock(EntityPlayer.class), null));
    }
}
