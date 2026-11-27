package am2.handlers;

import net.minecraftforge.client.event.RenderBlockOverlayEvent;

import am2.affinity.AffinityHelper;
import am2.items.ItemsCommonProxy;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Suppresses water/fire overlay effects when AM2 abilities or armor make them irrelevant. */
@SideOnly(Side.CLIENT)
public class BlockOverlayHandler {

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER) {
            if (AffinityHelper.isNotInWaterActually.contains(event.player)) {
                event.setCanceled(true);
            }
        } else if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            if (event.player != null && event.player.inventory.armorInventory[3] != null
                && event.player.inventory.armorInventory[3].getItem() == ItemsCommonProxy.archmageHood) {
                event.setCanceled(true);
            }
        }
    }
}
