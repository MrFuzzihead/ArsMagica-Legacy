package am2.handlers;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import am2.damage.DamageSources;
import am2.items.ItemOre;
import am2.items.ItemsCommonProxy;
import am2.utility.EntityUtilities;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Prevents summon drops and adds AM2's animal-fat drop. */
public class SummonDropHandler {

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (EntityUtilities.isSummon(event.entityLiving) && !(event.entityLiving instanceof EntityHorse)) {
            event.setCanceled(true);
        }
        if (event.source == DamageSources.darkNexus) {
            event.setCanceled(true);
        }
        if (!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPig
            && event.entityLiving.getRNG()
                .nextDouble() < 0.3f) {
            EntityItem animalFat = new EntityItem(event.entityLiving.worldObj);
            ItemStack stack = new ItemStack(ItemsCommonProxy.itemOre, 1, ItemOre.META_ANIMALFAT);
            animalFat.setPosition(event.entity.posX, event.entity.posY, event.entity.posZ);
            animalFat.setEntityItemStack(stack);
            event.drops.add(animalFat);
        }
    }
}
