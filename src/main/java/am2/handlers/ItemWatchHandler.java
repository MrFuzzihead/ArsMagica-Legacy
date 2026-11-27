package am2.handlers;

import net.minecraft.entity.item.EntityItemFrame;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import am2.AMCore;
import am2.EntityItemWatcher;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Registers item frames and dropped items with the compendium watchers. */
public class ItemWatchHandler {

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        if (!(event.entityLiving instanceof FakePlayer) && event.target instanceof EntityItemFrame) {
            AMCore.proxy.itemFrameWatcher.startWatchingFrame((EntityItemFrame) event.target);
        }
    }

    @SubscribeEvent
    public void onPlayerTossItem(ItemTossEvent event) {
        if (!event.entityItem.worldObj.isRemote) EntityItemWatcher.instance.addWatchedItem(event.entityItem);
    }
}
