package am2.handlers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;

import am2.api.spell.enums.BuffPowerLevel;
import am2.buffs.BuffList;
import am2.items.ItemsCommonProxy;
import am2.playerextensions.ExtendedProperties;

/** Applies agility, leap, entangled and ender-boot jump behavior. */
public class JumpHandler {

    public void onEntityJump(LivingJumpEvent event) {
        if (event.entityLiving.isPotionActive(BuffList.agility.id)) {
            event.entityLiving.motionY *= 1.5f;
        }
        if (event.entityLiving.isPotionActive(BuffList.leap.id)) {

            Entity velocityTarget = event.entityLiving;

            if (event.entityLiving.ridingEntity != null) {
                if (event.entityLiving.ridingEntity instanceof EntityMinecart) {
                    event.entityLiving.ridingEntity.setPosition(
                        event.entityLiving.ridingEntity.posX,
                        event.entityLiving.ridingEntity.posY + 1.5,
                        event.entityLiving.ridingEntity.posZ);
                }
                velocityTarget = event.entityLiving.ridingEntity;
            }

            double yVelocity = 0;
            double xVelocity = 0;
            double zVelocity = 0;

            Vec3 vec = event.entityLiving.getLookVec()
                .normalize();
            switch (event.entityLiving.getActivePotionEffect(BuffList.leap)
                .getAmplifier()) {
                case BuffPowerLevel.Low:
                    yVelocity = 0.4;
                    xVelocity = velocityTarget.motionX * 1.08 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.08 * Math.abs(vec.zCoord);
                    break;
                case BuffPowerLevel.Medium:
                    yVelocity = 0.7;
                    xVelocity = velocityTarget.motionX * 1.25 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.25 * Math.abs(vec.zCoord);
                    break;
                case BuffPowerLevel.High:
                    yVelocity = 1;
                    xVelocity = velocityTarget.motionX * 1.75 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.75 * Math.abs(vec.zCoord);
                    break;
                default:
                    break;
            }

            float maxHorizontalVelocity = 1.45f;

            if (event.entityLiving.ridingEntity != null
                && (event.entityLiving.ridingEntity instanceof EntityMinecart
                    || event.entityLiving.ridingEntity instanceof EntityBoat)
                || event.entityLiving.isPotionActive(BuffList.haste.id)) {
                maxHorizontalVelocity += 25;
                xVelocity *= 2.5;
                zVelocity *= 2.5;
            }

            if (xVelocity > maxHorizontalVelocity) {
                xVelocity = maxHorizontalVelocity;
            } else if (xVelocity < -maxHorizontalVelocity) {
                xVelocity = -maxHorizontalVelocity;
            }

            if (zVelocity > maxHorizontalVelocity) {
                zVelocity = maxHorizontalVelocity;
            } else if (zVelocity < -maxHorizontalVelocity) {
                zVelocity = -maxHorizontalVelocity;
            }

            if (ExtendedProperties.For(event.entityLiving)
                .getIsFlipped()) {
                yVelocity *= -1;
            }

            velocityTarget.addVelocity(xVelocity, yVelocity, zVelocity);
        }
        if (event.entityLiving.isPotionActive(BuffList.entangled.id)) {
            event.entityLiving.motionY = 0;
        }

        if (event.entityLiving instanceof EntityPlayer) {
            ItemStack boots = ((EntityPlayer) event.entityLiving).inventory.armorInventory[0];
            if (boots != null && boots.getItem() == ItemsCommonProxy.enderBoots && event.entityLiving.isSneaking()) {
                ExtendedProperties.For(event.entityLiving)
                    .toggleFlipped();
            }
        }
        if (ExtendedProperties.For(event.entityLiving)
            .getFlipRotation() > 0) event.entityLiving.addVelocity(0, -2 * event.entityLiving.motionY, 0);

    }

}
