package am2.handlers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSnowball;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import am2.blocks.tileentities.TileEntityInfusedStem;
import am2.bosses.BossSpawnHelper;
import am2.entities.EntityHallucination;
import am2.playerextensions.ExtendedProperties;
import am2.utility.EntityUtilities;

/** Death contingencies, summon cleanup, karma and infused-stem notification. */
public class EntityDeathHandler {

    public void onEntityDeath(LivingDeathEvent event) {
        EntityLivingBase soonToBeDead = event.entityLiving;
        if (!(ExtendedProperties.For(soonToBeDead)
            .getContingencyEffect(1)
            .getItem() instanceof ItemSnowball)) {
            ExtendedProperties.For(soonToBeDead)
                .procContingency(1, null);
        }

        if (soonToBeDead instanceof EntityHallucination && event.source.getSourceOfDamage() != null) {
            if (event.source.getSourceOfDamage() instanceof EntityPlayer) { // bad karma with the world
                ExtendedProperties exProps = ExtendedProperties.For((EntityPlayer) event.source.getSourceOfDamage());
                exProps.addToExtraVariables("karma", "bad");
            }
        } else if (soonToBeDead instanceof EntityCreature) {
            if (!EntityUtilities.isSummon(soonToBeDead) && EntityUtilities.isAIEnabled((EntityCreature) soonToBeDead)
                && event.source.getSourceOfDamage() instanceof EntityPlayer) {
                EntityUtilities.handleCrystalPhialAdd(
                    (EntityCreature) soonToBeDead,
                    (EntityPlayer) event.source.getSourceOfDamage());
            }
        }

        if (EntityUtilities.isSummon(soonToBeDead)) {
            int ownerID = EntityUtilities.getOwner(soonToBeDead);
            Entity e = soonToBeDead.worldObj.getEntityByID(ownerID);
            if (e != null & e instanceof EntityLivingBase) {
                ExtendedProperties.For((EntityLivingBase) e)
                    .removeSummon();
            }
        }

        if (soonToBeDead instanceof EntityVillager) {
            BossSpawnHelper.instance.onVillagerChildKilled((EntityVillager) soonToBeDead);
        }

        if (soonToBeDead instanceof EntityLiving && !soonToBeDead.worldObj.isRemote) {
            int x = (int) Math.floor(soonToBeDead.posX);
            int y = (int) Math.floor(soonToBeDead.posY);
            int z = (int) Math.floor(soonToBeDead.posZ);
            for (int newx = x - 3; newx <= x + 3; newx++) {
                for (int newy = y - 3; newy <= y + 3; newy++) {
                    for (int newz = z - 3; newz <= z + 3; newz++) {
                        if (soonToBeDead.worldObj.getTileEntity(newx, newy, newz) instanceof TileEntityInfusedStem) {
                            ((TileEntityInfusedStem) soonToBeDead.worldObj
                                .getTileEntity(newx, newy, newz)).killedEntities.add((EntityLiving) soonToBeDead);
                            return;
                        }
                    }
                }
            }
        }
    }

}
