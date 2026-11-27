package am2.handlers;

import java.util.ListIterator;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import am2.enchantments.AMEnchantments;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Keeps soulbound items through death and respawn. */
public class SoulboundItemsHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDropsEvent evt) {
        if (evt.entityPlayer == null || evt.entityPlayer instanceof FakePlayer || evt.isCanceled()) {
            return;
        }
        if (evt.entityPlayer.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory")) {
            return;
        }
        ListIterator<EntityItem> iter = evt.drops.listIterator();
        while (iter.hasNext()) {
            EntityItem ei = iter.next();
            ItemStack item = ei.getEntityItem();
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    iter.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone evt) {
        if (!evt.wasDeath || evt.isCanceled()) {
            return;
        }
        if (evt.original == null || evt.entityPlayer == null || evt.entityPlayer instanceof FakePlayer) {
            return;
        }
        if (evt.entityPlayer.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory")) {
            return;
        }
        for (int i = 0; i < evt.original.inventory.mainInventory.length; i++) {
            ItemStack item = evt.original.inventory.mainInventory[i];
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    evt.original.inventory.mainInventory[i] = null;
                }
            }
        }
        for (int i = 0; i < evt.original.inventory.armorInventory.length; i++) {
            ItemStack item = evt.original.inventory.armorInventory[i];
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    evt.original.inventory.armorInventory[i] = null;
                }
            }
        }
    }

    private boolean isSoulBound(ItemStack item) {
        return EnchantmentHelper.getEnchantmentLevel(AMEnchantments.soulbound.effectId, item) > 0;
    }

    static boolean addToPlayerInventory(EntityPlayer entityPlayer, ItemStack item) {
        if (item == null || entityPlayer == null) {
            return false;
        }
        if (item.getItem() instanceof ItemArmor) {
            ItemArmor arm = (ItemArmor) item.getItem();
            int index = 3 - arm.armorType;
            if (entityPlayer.inventory.armorItemInSlot(index) == null) {
                entityPlayer.inventory.armorInventory[index] = item;
                return true;
            }
        }
        InventoryPlayer inv = entityPlayer.inventory;
        for (int i = 0; i < inv.mainInventory.length; i++) {
            if (inv.mainInventory[i] == null) {
                inv.mainInventory[i] = item.copy();
                return true;
            }
        }
        return false;
    }
}
