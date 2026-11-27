package am2.handlers;

import am2.AMCore;
import am2.utility.TickrateController;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

/** Keeps client/server tickrate state aligned across connection changes. */
public class ConnectionTickrateHandler {

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        TickrateController.setServerTickrate(TickrateController.DEFAULT_TICKS_PER_SECOND);
        TickrateController.changeClientTickratePublic(null, TickrateController.DEFAULT_TICKS_PER_SECOND);
        AMCore.proxy.itemFrameWatcher.clear();
        am2.AMEventHandler.clearAllTransientState();
    }

    @SubscribeEvent
    public void connect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (event.isLocal) {
            float tickrate = TickrateController.DEFAULT_TICKS_PER_SECOND;
            TickrateController.setServerTickrate(tickrate);
            TickrateController.changeClientTickratePublic(null, tickrate);
        } else {
            TickrateController.changeClientTickratePublic(null, TickrateController.DEFAULT_TICKS_PER_SECOND);
        }
    }

    @SubscribeEvent
    public void connect(PlayerEvent.PlayerLoggedInEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            float tickrate = (float) 1000L / TickrateController.getServerTickrate();
            TickrateController.changeClientTickratePublic(event.player, tickrate);
        }
    }
}
