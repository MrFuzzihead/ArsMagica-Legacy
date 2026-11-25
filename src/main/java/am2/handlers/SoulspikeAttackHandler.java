package am2.handlers;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import am2.AMCore;
import am2.entities.EntityHallucination;
import am2.items.ItemSoulspike;
import am2.playerextensions.ExtendedProperties;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Soulspike on-hit mana gain and bonus damage. */
public class SoulspikeAttackHandler {

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer.getHeldItem() != null) {
            if (event.entityPlayer.getHeldItem()
                .getItem() instanceof ItemSoulspike) {
                if (event.target instanceof EntityHallucination) {
                    ItemSoulspike.addManaToSpike(event.entityPlayer.getHeldItem(), 15);
                    event.target.attackEntityFrom(DamageSource.outOfWorld, 15);
                    event.target.attackEntityFrom(DamageSource.magic, 15);
                } else if (event.target instanceof EntityLivingBase) {
                    if (event.target instanceof EntityPlayer) {
                        EntityPlayer pl = (EntityPlayer) event.target;
                        if (!AMCore.config.getAllowCreativeTargets() && pl.capabilities.isCreativeMode) return;
                        ExtendedProperties properties = ExtendedProperties.For(pl);
                        if (properties.hasExtraVariable("ethereal")) {
                            ItemSoulspike.addManaToSpike(
                                event.entityPlayer.getHeldItem(),
                                (int) (properties.getCurrentMana() / 4));
                            properties.deductMana(properties.getCurrentMana() / 4);
                        }
                    }
                    ItemSoulspike.addManaToSpike(event.entityPlayer.getHeldItem(), 3);
                    event.target.attackEntityFrom(DamageSource.outOfWorld, 3);
                }
            }
        }
    }
}
