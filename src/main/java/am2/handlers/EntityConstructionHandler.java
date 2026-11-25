package am2.handlers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

import am2.AMCore;
import am2.api.ArsMagicaApi;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Registers AM2's extended properties and attributes on every new entity. */
public class EntityConstructionHandler {

    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {
        if (event.entity instanceof EntityLivingBase) {
            EntityLivingBase entityLB = (EntityLivingBase) event.entity;
            entityLB.registerExtendedProperties(ExtendedProperties.identifier, new ExtendedProperties());
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.maxManaBonus);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.maxBurnoutBonus);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.xpGainModifier);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.burnoutReductionRate);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.manaRegenTimeModifier);
            ExtendedProperties.For(entityLB)
                .register(event.entity.worldObj, entityLB);
            if (event.entity instanceof EntityPlayer) {
                event.entity.registerExtendedProperties(RiftStorage.identifier, new RiftStorage());
                event.entity.registerExtendedProperties(AffinityData.identifier, new AffinityData());
                event.entity
                    .registerExtendedProperties(SkillData.identifier, new SkillData((EntityPlayer) event.entity));
            }
        } else if (event.entity instanceof EntityItemFrame) {
            AMCore.proxy.itemFrameWatcher.startWatchingFrame((EntityItemFrame) event.entity);
        }
    }
}
