package am2.handlers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import am2.buffs.BuffList;
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
}
