package am2.handlers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import am2.buffs.BuffList;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Fury accelerates block breaking. */
public class BreakSpeedHandler {

    @SubscribeEvent
    public void onBreakSpeed(BreakSpeed event) {
        EntityPlayer player = event.entityPlayer;
        if (player.isPotionActive(BuffList.fury.id)) event.newSpeed = event.originalSpeed * 5;
    }
}
