package am2.proxy.tick;

import static am2.AMEventHandler.tempCurseMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;

import am2.AMCore;
import am2.AMEventHandler;
import am2.EntityItemWatcher;
import am2.MeteorSpawnHelper;
import am2.bosses.BossSpawnHelper;
import am2.items.ItemsCommonProxy;
import am2.network.AMDataWriter;
import am2.network.AMNetHandler;
import am2.network.AMPacketIDs;
import am2.spell.SpellHelper;
import am2.utility.DimensionUtilities;
import am2.utility.TickrateController;
import am2.worldgen.RetroactiveWorldgenerator;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ServerTickHandler {

    private boolean firstTick = true;
    public static HashMap<EntityLiving, EntityLivingBase> targetsToSet = new HashMap<EntityLiving, EntityLivingBase>();

    public static String lastWorldName;

    private void gameTick_Start() {

        if (MinecraftServer.getServer()
            .getFolderName() != lastWorldName) {
            lastWorldName = MinecraftServer.getServer()
                .getFolderName();
            firstTick = true;
        }

        if (firstTick) {
            ItemsCommonProxy.crystalPhylactery.getSpawnableEntities(MinecraftServer.getServer().worldServers[0]);
            firstTick = false;
        }

        AMCore.proxy.itemFrameWatcher.checkWatchedFrames(false);
    }

    private void gameTick_End() {
        BossSpawnHelper.instance.tick();
        MeteorSpawnHelper.instance.tick();
        EntityItemWatcher.instance.tick();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            gameTick_Start();
        } else if (event.phase == TickEvent.Phase.END) {
            gameTick_End();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        SpellHelper.clearLingeringSpells(event.world);
        AMCore.proxy.itemFrameWatcher.clearWorld(event.world);
        AMEventHandler.clearWorldState(event.world);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote && AMCore.config.retroactiveWorldgen()) {
            RetroactiveWorldgenerator.instance.continueRetrogen(event.world);
        }

        // WorldTickEvent is also fired for the client world, which in an integrated server means it
        // can arrive on the client thread. Both queues below end up mutating server entities, so
        // neither may be drained anywhere but the server thread.
        if (!event.world.isRemote) {
            applyDeferredPotionEffects();
            if (event.phase == TickEvent.Phase.END) {
                applyDeferredDimensionTransfers();
            }
        }

        // Update lingering spells exactly once per world tick, on the server only.
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            SpellHelper.tickLingeringSpells(event.world);
        }

        // handle temporary curses
        ArrayList<EntityCreature> toRemove = new ArrayList<EntityCreature>();
        HashMap<EntityCreature, Integer> toChange = new HashMap<EntityCreature, Integer>();
        for (Map.Entry<EntityCreature, Integer> entry : tempCurseMap.entrySet()) {
            EntityCreature key = entry.getKey();
            Integer value = entry.getValue();
            if (value <= 3) toRemove.add(key);
            else toChange.put(key, value - 1);
        }
        for (Map.Entry<EntityCreature, Integer> entry : toChange.entrySet()) {
            tempCurseMap.put(entry.getKey(), entry.getValue()); // overwrite with new value
        }
        for (EntityCreature ec : toRemove) {
            tempCurseMap.remove(ec);
            ec.setDead();
        }

        MinecraftServer server = MinecraftServer.getServer();
        if ((server != null) && (server.getConfigurationManager() != null)) {
            if (TickrateController.countdownToChangeBack >= 3) {
                TickrateController.countdownToChangeBack--;
            } else if (TickrateController.countdownToChangeBack != -1) {
                TickrateController.countdownToChangeBack = -1;
                TickrateController.changeTickrate(TickrateController.DEFAULT_TICKS_PER_SECOND);
            }
        }
    }

    private void applyDeferredPotionEffects() {
        for (Map.Entry<EntityLivingBase, List<PotionEffect>> entry : AMCore.proxy.drainDeferredPotionEffects()
            .entrySet()) {
            EntityLivingBase ent = entry.getKey();
            if (ent == null || ent.isDead) continue;
            for (PotionEffect effect : entry.getValue()) {
                ent.addPotionEffect(effect);
            }
        }
    }

    private void applyDeferredDimensionTransfers() {
        for (Map.Entry<EntityLivingBase, Integer> entry : AMCore.proxy.drainDeferredDimensionTransfers()
            .entrySet()) {
            DimensionUtilities.doDimensionTransfer(entry.getKey(), entry.getValue());
        }
    }

    private void applyDeferredTargetSets() {
        Iterator<Entry<EntityLiving, EntityLivingBase>> it = targetsToSet.entrySet()
            .iterator();
        while (it.hasNext()) {
            Entry<EntityLiving, EntityLivingBase> entry = it.next();
            if (entry.getKey() != null && !entry.getKey().isDead) entry.getKey()
                .setAttackTarget(entry.getValue());
            it.remove();
        }
    }

    public void addDeferredTarget(EntityLiving ent, EntityLivingBase target) {
        targetsToSet.put(ent, target);
    }

    public void blackoutArmorPiece(EntityPlayerMP player, int slot, int cooldown) {
        AMNetHandler.INSTANCE.sendPacketToClientPlayer(
            player,
            AMPacketIDs.FLASH_ARMOR_PIECE,
            new AMDataWriter().add(slot)
                .add(cooldown)
                .generate());
    }

}
