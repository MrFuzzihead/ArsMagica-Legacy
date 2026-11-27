package am2.network;

import am2.utility.ClientTickrateController;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class TickrateMessageHandler implements IMessageHandler<TickrateMessage, IMessage> {

    @Override
    public IMessage onMessage(TickrateMessage msg, MessageContext context) {
        float tickrate = msg.getTickrate();
        if (FMLCommonHandler.instance()
            .getSide() != Side.SERVER) {
            ClientTickrateController.changeClientTickrate(tickrate);
        }
        return null;
    }

}
