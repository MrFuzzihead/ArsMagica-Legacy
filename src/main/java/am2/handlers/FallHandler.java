package am2.handlers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import am2.api.spell.enums.Affinity;
import am2.buffs.BuffList;
import am2.items.ItemsCommonProxy;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Slowfall, archmage boots and fall-protection handling. */
public class FallHandler {

    @SubscribeEvent
    public void onEntityFall(LivingFallEvent event) {
        EntityLivingBase ent = event.entityLiving;
        float f = event.distance;
        ent.isAirBorne = false;

        if (ent.isPotionActive(BuffList.slowfall.id) || ent.isPotionActive(BuffList.shrink.id)
            || (ent instanceof EntityPlayer && AffinityData.For(ent)
                .getAffinityDepth(Affinity.NATURE) == 1.0f)) {
            event.setCanceled(true);
            ent.fallDistance = 0;
            return;
        }

        if (ent instanceof EntityPlayer && ((EntityPlayer) ent).inventory.armorInventory[0] != null) {
            if (((EntityPlayer) ent).inventory.armorInventory[0].getItem() == ItemsCommonProxy.archmageBoots) {
                event.setCanceled(true);
                return;
            }
        }

        if (ent.isPotionActive(BuffList.gravityWell.id)) {
            ent.fallDistance *= 1.5f;
        }

        f -= ExtendedProperties.For(ent)
            .getFallProtection();
        ExtendedProperties.For(ent)
            .setFallProtection(0);
        if (f <= 0) {
            ent.fallDistance = 0;
            event.setCanceled(true);
        }
    }
}
