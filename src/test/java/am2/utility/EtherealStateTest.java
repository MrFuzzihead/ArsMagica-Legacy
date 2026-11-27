package am2.utility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/**
 * Covers the ethereal-boots lookup now that it lives outside the old coremod class.
 */
public class EtherealStateTest {

    @Test
    public void nullPlayerIsNotEthereal() {
        assertFalse(EtherealState.isEthereal(null));
    }

    @Test
    public void playerWithoutInventoryIsNotEthereal() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = null;
        assertFalse(EtherealState.isEthereal(player));
    }

    @Test
    public void playerWithoutBootsIsNotEthereal() {
        EntityPlayer player = playerWithBoots(null);
        assertFalse(EtherealState.isEthereal(player));
    }

    @Test
    public void bootsWithoutDisplayTagAreNotEthereal() {
        ItemStack boots = mock(ItemStack.class);
        boots.stackTagCompound = new NBTTagCompound();
        assertFalse(EtherealState.isEthereal(playerWithBoots(boots)));
    }

    @Test
    public void bootsWithDisplayButNoEtherealFlagAreNotEthereal() {
        ItemStack boots = bootsWithEthereal(false);
        assertFalse(EtherealState.isEthereal(playerWithBoots(boots)));
    }

    @Test
    public void bootsWithEtherealFlagAreEthereal() {
        ItemStack boots = bootsWithEthereal(true);
        assertTrue(EtherealState.isEthereal(playerWithBoots(boots)));
    }

    private static EntityPlayer playerWithBoots(ItemStack boots) {
        EntityPlayer player = mock(EntityPlayer.class);
        InventoryPlayer inventory = mock(InventoryPlayer.class);
        inventory.armorInventory = new ItemStack[] { boots, null, null, null };
        player.inventory = inventory;
        return player;
    }

    private static ItemStack bootsWithEthereal(boolean ethereal) {
        ItemStack boots = mock(ItemStack.class);
        boots.stackTagCompound = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setBoolean("ethereal", ethereal);
        boots.stackTagCompound.setTag("display", display);
        return boots;
    }
}
