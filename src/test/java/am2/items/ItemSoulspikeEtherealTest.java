package am2.items;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;

import org.junit.Test;

import am2.playerextensions.ExtendedProperties;

public class ItemSoulspikeEtherealTest {

    @Test
    public void clearingEtherealRestoresCapabilitiesAndRemovesTheVariable() {
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = mock(PlayerCapabilities.class);
        player.capabilities.disableDamage = true;
        player.capabilities.allowEdit = false;
        player.capabilities.isFlying = true;
        player.inventory = mock(InventoryPlayer.class);
        player.inventory.armorInventory = new net.minecraft.item.ItemStack[4];
        ExtendedProperties properties = mock(ExtendedProperties.class);

        ItemSoulspike.clearEtherealForm(player, properties);

        org.junit.Assert.assertFalse(player.capabilities.disableDamage);
        org.junit.Assert.assertTrue(player.capabilities.allowEdit);
        org.junit.Assert.assertFalse(player.capabilities.isFlying);
        org.junit.Assert.assertFalse(player.noClip);
        verify(properties).removeFromExtraVariables("ethereal");
    }
}
