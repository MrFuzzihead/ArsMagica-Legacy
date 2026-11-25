package am2.handlers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSnowball;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import am2.AMCore;
import am2.AMEventHandler;
import am2.buffs.BuffList;
import am2.damage.DamageSourceFire;
import am2.items.ItemsCommonProxy;
import am2.network.AMNetHandler;
import am2.playerextensions.ExtendedProperties;

/** Incoming damage modifiers from armor, shields, contingencies and buffs. */
public class LivingHurtHandler {

    private double oldKnockbackValue = -1;

    public void onEntityHurt(LivingHurtEvent event) {

        if (event.source.isFireDamage() && event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem() == ItemsCommonProxy.fireEars
                || ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                    == ItemsCommonProxy.archmageHood) {
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

        if (event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[2] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.archmageArmor) {
                if (oldKnockbackValue == -1) {
                    oldKnockbackValue = event.entityLiving
                        .getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                        .getAttributeValue();
                }
                event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                    .setBaseValue(1);
            } else {
                event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                    .setBaseValue(oldKnockbackValue);
                oldKnockbackValue = -1;
            }
        } else {
            event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                .setBaseValue(oldKnockbackValue);
            oldKnockbackValue = -1;
        }

        PotionEffect magicShield = event.entityLiving.getActivePotionEffect(BuffList.magicShield);
        if (magicShield != null) {
            event.ammount *= 0.5f;
            event.ammount *= (1f / (magicShield.getAmplifier() + 1f));
        }

        if (event.entityLiving.isPotionActive(BuffList.manaShield)) {
            float manaToTake = Math.min(
                ExtendedProperties.For(event.entityLiving)
                    .getCurrentMana(),
                event.ammount * 250f);
            event.ammount -= manaToTake / 250f;
            ExtendedProperties.For(event.entityLiving)
                .deductMana(manaToTake);
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
                    event.entityLiving.posY + event.entityLiving.getEyeHeight()
                        - 1
                        + event.entityLiving.worldObj.rand.nextFloat() * 2,
                    event.entityLiving.posZ - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                    6,
                    -1);
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.arcane",
                1.0f,
                event.entityLiving.worldObj.rand.nextFloat() + 0.5f);
            if (event.ammount <= 0) {
                event.setCanceled(true);
                return;
            }
        }

        Entity entityAttacker = event.source.getSourceOfDamage();
        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.earthGuardianArmor
            && ((EntityPlayer) entityAttacker).getCurrentEquippedItem() == null) {
            event.ammount += 4;

            double deltaZ = event.entityLiving.posZ - entityAttacker.posZ;
            double deltaX = event.entityLiving.posX - entityAttacker.posX;
            double angle = Math.atan2(deltaZ, deltaX);
            double speed = entityAttacker.isSprinting() ? 3 : 2;
            double vertSpeed = entityAttacker.isSprinting() ? 0.5 : 0.325;

            if (event.entityLiving instanceof EntityPlayer) {
                AMNetHandler.INSTANCE.sendVelocityAddPacket(
                    event.entityLiving.worldObj,
                    event.entityLiving,
                    speed * Math.cos(angle),
                    vertSpeed,
                    speed * Math.sin(angle));
            } else {
                event.entityLiving.motionX += (speed * Math.cos(angle));
                event.entityLiving.motionZ += (speed * Math.sin(angle));
                event.entityLiving.motionY += vertSpeed;
            }
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.earth",
                0.4f,
                event.entityLiving.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }
        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.archmageArmor) {
            event.ammount += 5;

            double deltaZ = event.entityLiving.posZ - entityAttacker.posZ;
            double deltaX = event.entityLiving.posX - entityAttacker.posX;
            double angle = Math.atan2(deltaZ, deltaX);
            double speed = entityAttacker.isSprinting() ? 5 : 3;
            double vertSpeed = entityAttacker.isSprinting() ? 1 : 0.65;

            if (event.entityLiving instanceof EntityPlayer) {
                AMNetHandler.INSTANCE.sendVelocityAddPacket(
                    event.entityLiving.worldObj,
                    event.entityLiving,
                    speed * Math.cos(angle),
                    vertSpeed,
                    speed * Math.sin(angle));
            } else {
                event.entityLiving.motionX += (speed * Math.cos(angle));
                event.entityLiving.motionZ += (speed * Math.sin(angle));
                event.entityLiving.motionY += vertSpeed;
            }
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.earth",
                0.4f,
                event.entityLiving.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        ExtendedProperties extendedProperties = ExtendedProperties.For(event.entityLiving);
        EntityLivingBase entityAttacked = event.entityLiving;
        if (!(extendedProperties.getContingencyEffect(0)
            .getItem() instanceof ItemSnowball)) {
            extendedProperties.procContingency(
                0,
                (event.source.getEntity() != null && event.source.getEntity() instanceof EntityLivingBase)
                    ? (EntityLivingBase) event.source.getEntity()
                    : null);
        }
        if (!(extendedProperties.getContingencyEffect(4)
            .getItem() instanceof ItemSnowball) && entityAttacked.getHealth() <= entityAttacked.getMaxHealth() / 3) {
            extendedProperties.procContingency(
                4,
                (event.source.getEntity() != null && event.source.getEntity() instanceof EntityLivingBase)
                    ? (EntityLivingBase) event.source.getEntity()
                    : null);
        }

        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[3] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[1] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[0] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[3].getItem() == ItemsCommonProxy.archmageHood
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem() == ItemsCommonProxy.archmageArmor
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[1].getItem()
                == ItemsCommonProxy.archmageLeggings
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[0].getItem()
                == ItemsCommonProxy.archmageBoots) {
            if (entityAttacker.isSneaking()) {
                extendedProperties.toggleFlipped();
                AMEventHandler.setTempFlipped(extendedProperties, 50);
            }
        }

        if (entityAttacked.isPotionActive(BuffList.fury.id)) event.ammount /= 2;

        if (entityAttacker instanceof EntityLivingBase
            && ((EntityLivingBase) entityAttacker).isPotionActive(BuffList.shrink)) event.ammount /= 2;
    }

}
