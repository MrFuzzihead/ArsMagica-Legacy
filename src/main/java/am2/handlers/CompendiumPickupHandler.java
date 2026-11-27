package am2.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

import am2.AMCore;
import am2.items.ItemsCommonProxy;
import am2.network.AMNetHandler;
import am2.playerextensions.ExtendedProperties;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Handles compendium unlocking when items are picked up. */
public class CompendiumPickupHandler {

    @SubscribeEvent
    public void onPlayerPickupItem(EntityItemPickupEvent event) {
        if (event.entityPlayer == null) return;

        if (!event.entityPlayer.worldObj.isRemote && ExtendedProperties.For(event.entityPlayer)
            .getMagicLevel() <= 0
            && event.item.getEntityItem()
                .getItem() == ItemsCommonProxy.arcaneCompendium) {
            event.entityPlayer.addChatMessage(new ChatComponentText("You have unlocked the secrets of the arcane!"));
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "shapes", true);
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "components", true);
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "modifiers", true);
            ExtendedProperties.For(event.entityPlayer)
                .setMagicLevelWithMana(1);
            ExtendedProperties.For(event.entityPlayer)
                .forceSync();
            return;
        }

        ItemStack stack = event.item.getEntityItem();
        Item item = stack.getItem();
        if (item == ItemsCommonProxy.spell) {
            if (event.entityPlayer.worldObj.isRemote) {
                AMNetHandler.INSTANCE
                    .sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "spell_book", false);
            }
        } else {
            int meta = stack.getItemDamage();

            if (event.entityPlayer.worldObj.isRemote && item.getUnlocalizedName() != null
                && (AMCore.proxy.items.getArsMagicaItems()
                    .contains(item))
                || (item instanceof ItemBlock && AMCore.proxy.blocks.getArsMagicaBlocks()
                    .contains(((ItemBlock) item).field_150939_a))) {
                AMNetHandler.INSTANCE.sendCompendiumUnlockPacket(
                    (EntityPlayerMP) event.entityPlayer,
                    item.getUnlocalizedName()
                        .replace("item.", "")
                        .replace("arsmagica2:", "")
                        .replace("tile.", "") + ((meta > -1) ? "@" + meta : ""),
                    false);
            }
        }
    }
}
