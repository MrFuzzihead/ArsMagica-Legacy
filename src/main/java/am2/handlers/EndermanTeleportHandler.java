package am2.handlers;

import java.util.ArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import am2.api.spell.enums.Affinity;
import am2.blocks.tileentities.TileEntityAstralBarrier;
import am2.buffs.BuffList;
import am2.entities.EntityFlicker;
import am2.utility.DimensionUtilities;
import am2.utility.KeystoneUtilities;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Astral-barrier and enderman flicker teleport behavior. */
public class EndermanTeleportHandler {

    @SubscribeEvent
    public void onEndermanTeleport(EnderTeleportEvent event) {
        EntityLivingBase ent = event.entityLiving;

        ArrayList<Long> keystoneKeys = KeystoneUtilities.instance.GetKeysInInvenory(ent);
        TileEntityAstralBarrier blockingBarrier = DimensionUtilities.GetBlockingAstralBarrier(
            event.entityLiving.worldObj,
            (int) event.targetX,
            (int) event.targetY,
            (int) event.targetZ,
            keystoneKeys);

        if (ent.isPotionActive(BuffList.astralDistortion.id) || blockingBarrier != null) {
            event.setCanceled(true);
            if (blockingBarrier != null) {
                blockingBarrier.onEntityBlocked(ent);
            }
            return;
        }

        if (!ent.worldObj.isRemote && ent instanceof EntityEnderman && ent.worldObj.rand.nextDouble() < 0.01f) {
            EntityFlicker flicker = new EntityFlicker(ent.worldObj);
            flicker.setPosition(ent.posX, ent.posY, ent.posZ);
            flicker.setFlickerType(Affinity.ENDER);
            ent.worldObj.spawnEntityInWorld(flicker);
        }
    }
}
