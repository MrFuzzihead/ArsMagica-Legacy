package am2.handlers;

import java.util.List;

import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.brewing.PotionBrewedEvent;

import am2.buffs.BuffList;
import am2.items.ItemsCommonProxy;
import am2.utility.InventoryUtilities;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Replaces vanilla potion results with AM2's mana potions. */
public class PotionBrewingHandler {

    @SubscribeEvent
    public void onPotionBrewed(PotionBrewedEvent brewEvent) {
        for (ItemStack stack : brewEvent.brewingStacks) {
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemPotion) {
                ItemPotion ptn = ((ItemPotion) stack.getItem());
                List<PotionEffect> fx = ptn.getEffects(stack.getItemDamage());
                if (fx == null) return;
                for (PotionEffect pe : fx) {
                    if (pe.getPotionID() == BuffList.greaterManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.greaterManaPotion);
                        break;
                    } else if (pe.getPotionID() == BuffList.epicManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.epicManaPotion);
                        break;
                    } else if (pe.getPotionID() == BuffList.legendaryManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.legendaryManaPotion);
                        break;
                    }
                }
            }
        }
    }
}
