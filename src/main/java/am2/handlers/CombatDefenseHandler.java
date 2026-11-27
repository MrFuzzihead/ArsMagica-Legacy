package am2.handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import am2.AMCore;
import am2.buffs.BuffList;
import am2.damage.DamageSourceFire;
import am2.items.ItemsCommonProxy;
import am2.playerextensions.ExtendedProperties;

/** Fire armor retaliation and mana-shield absorption. */
public class CombatDefenseHandler {

    public void onEntityAttacked(LivingAttackEvent event) {
        if (event.source.isFireDamage() && event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem() == ItemsCommonProxy.fireEars
                || ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                    == ItemsCommonProxy.archmageHood) {
                event.entityLiving.setFire(0);
                event.setCanceled(true);
                return;
            }
        } else if (event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                == ItemsCommonProxy.archmageHood) {
                    if (event.source.getEntity() != null) {
                        event.source.getEntity()
                            .attackEntityFrom(
                                new DamageSourceFire(event.entityLiving),
                                event.entityLiving.worldObj.rand.nextInt(3));
                        event.source.getEntity()
                            .setFire(8);
                    }
                }

        if (event.entityLiving.isPotionActive(BuffList.manaShield)) {
            if (ExtendedProperties.For(event.entityLiving)
                .getCurrentMana() >= event.ammount * 250f) {
                ExtendedProperties.For(event.entityLiving)
                    .deductMana(event.ammount * 100f);
                ExtendedProperties.For(event.entityLiving)
                    .forceSync();
                for (int i = 0; i < Math.min(event.ammount, 5 * AMCore.config.getGFXLevel()); ++i)
                    AMCore.proxy.particleManager.BoltFromPointToPoint(
                        event.entityLiving.worldObj,
                        event.entityLiving.posX,
                        event.entityLiving.posY
                            + event.entityLiving.worldObj.rand.nextFloat() * event.entityLiving.getEyeHeight(),
                        event.entityLiving.posZ,
                        event.entityLiving.posX - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        event.entityLiving.posY - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        event.entityLiving.posZ - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        6,
                        -1);
                event.entityLiving.worldObj.playSoundAtEntity(
                    event.entityLiving,
                    "arsmagica2:spell.cast.arcane",
                    1.0f,
                    event.entityLiving.worldObj.rand.nextFloat() + 0.5f);
                event.setCanceled(true);
            }
        }
    }

}
