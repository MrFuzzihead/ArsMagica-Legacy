package am2;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import am2.customdata.CustomWorldData;
import am2.network.AMDataWriter;
import am2.network.AMNetHandler;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.SkillData;
import am2.proxy.tick.ServerTickHandler;
import am2.spell.SkillTreeManager;
import am2.utility.EntityUtilities;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

public class PlayerTracker {

    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (hasAA(event.player)) {
            AMNetHandler.INSTANCE.requestClientAuras((EntityPlayerMP) event.player);
        }

        int[] disabledSkills = SkillTreeManager.instance.getDisabledSkillIDs();

        AMDataWriter writer = new AMDataWriter();
        writer.add(AMCore.config.getSkillTreeSecondaryTierCap())
            .add(disabledSkills);
        writer.add(AMCore.config.getManaCap());
        byte[] data = writer.generate();

        CustomWorldData.syncAllWorldVarsToClients(event.player);

        AMNetHandler.INSTANCE.syncLoginData((EntityPlayerMP) event.player, data);
        if (ServerTickHandler.lastWorldName != null)
            AMNetHandler.INSTANCE.syncWorldName((EntityPlayerMP) event.player, ServerTickHandler.lastWorldName);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        // kill any summoned creatures
        if (!event.player.worldObj.isRemote) {
            List list = event.player.worldObj.loadedEntityList;
            for (Object o : list) {
                if (o instanceof EntityLivingBase && EntityUtilities.isSummon((EntityLivingBase) o)
                    && EntityUtilities.getOwner((EntityLivingBase) o) == event.player.getEntityId()) {
                    ((EntityLivingBase) o).setDead();
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        // kill any summoned creatures, eventually respawn them in the new dimension
        if (!event.player.worldObj.isRemote) {
            List list = event.player.worldObj.loadedEntityList;
            for (Object o : list) {
                if (o instanceof EntityLivingBase && EntityUtilities.isSummon((EntityLivingBase) o)
                    && EntityUtilities.getOwner((EntityLivingBase) o) == event.player.getEntityId()) {
                    ((EntityLivingBase) o).setDead();
                }
            }
            ExtendedProperties.For(event.player)
                .setDelayedSync(40);
            AffinityData.For(event.player)
                .setDelayedSync(40);
            SkillData.For(event.player)
                .setDelayedSync(40);
        }
    }

    public static boolean hasAA(EntityPlayer entity) {
        return getAAL(entity);
    }

    public static boolean getAAL(EntityPlayer thePlayer) {
        if (thePlayer == null) return false;
        String name = "";
        try {
            name = thePlayer.getDisplayName();
        } catch (Exception ignored) {

        }
        return name.equalsIgnoreCase("Nlghtwing");
    }
}
