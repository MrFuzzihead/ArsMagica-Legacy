package am2.handlers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import am2.AMEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Applies extra entity ticks once per server tick. */
public class AcceleratedEntityTickHandler {

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !AMEventHandler.isAccelerateEnabled()
            || AMEventHandler.acceleratedEntitiesUUIDs.isEmpty()) {
            return;
        }

        for (WorldServer worldServer : DimensionManager.getWorlds()) {
            List<Entity> entityList = new ArrayList<>(worldServer.loadedEntityList);
            for (Entity entity : entityList) {
                if (!(entity instanceof EntityLivingBase) || entity.isDead) {
                    continue;
                }

                Integer extraTicks = AMEventHandler.acceleratedEntitiesUUIDs.get(entity.getUniqueID());
                if (extraTicks == null || extraTicks <= 0) {
                    continue;
                }

                for (int i = 0; i < extraTicks && !entity.isDead; i++) {
                    entity.onUpdate();
                }
            }
        }
    }
}
