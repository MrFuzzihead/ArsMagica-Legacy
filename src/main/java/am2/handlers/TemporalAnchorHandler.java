package am2.handlers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import am2.buffs.BuffEffectTemporalAnchor;
import am2.buffs.BuffList;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Temporal anchor is a respawn-only effect and must not survive an entity joining a world. */
public class TemporalAnchorHandler {

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityLivingBase
            && ((EntityLivingBase) event.entity).isPotionActive(BuffList.temporalAnchor.id)) {
            ((EntityLivingBase) event.entity).removePotionEffect(BuffList.temporalAnchor.id);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDeathChrono(LivingDeathEvent event) {
        EntityLivingBase soonToBeDead = event.entityLiving;
        if (soonToBeDead.isPotionActive(BuffList.temporalAnchor.id)) {
            event.setCanceled(true);
            PotionEffect pe = soonToBeDead.getActivePotionEffect(BuffList.temporalAnchor);
            if (pe instanceof BuffEffectTemporalAnchor) {
                BuffEffectTemporalAnchor buff = (BuffEffectTemporalAnchor) pe;
                buff.stopEffect(soonToBeDead);
            }
            soonToBeDead.removePotionEffect(BuffList.temporalAnchor.id);
        }
    }
}
