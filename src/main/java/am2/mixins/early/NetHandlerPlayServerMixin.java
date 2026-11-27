package am2.mixins.early;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C0EPacketClickWindow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.utility.EtherealState;

/**
 * Replacement for the {@code NetHandlerPlayServer.processClickWindow} {@code @Fix}.
 *
 * <p>
 * An ethereal player has no inventory interaction (their client cannot see the open container consistently), so the
 * server sends a fresh container snapshot and ignores the click instead of processing a desynchronised slot.
 * </p>
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class NetHandlerPlayServerMixin {

    @Shadow
    public EntityPlayerMP playerEntity;

    @Inject(method = "processClickWindow", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealClickProtection(C0EPacketClickWindow packet, CallbackInfo ci) {
        if (!EtherealState.isEthereal(playerEntity)) {
            return;
        }

        playerEntity.func_143004_u();
        if (playerEntity.openContainer.windowId == packet.func_149548_c()
            && playerEntity.openContainer.isPlayerNotUsingContainer(playerEntity)) {
            ArrayList<ItemStack> contents = new ArrayList<ItemStack>();
            for (int i = 0; i < playerEntity.openContainer.inventorySlots.size(); ++i) {
                contents.add(((Slot) playerEntity.openContainer.inventorySlots.get(i)).getStack());
            }
            playerEntity.sendContainerAndContentsToPlayer(playerEntity.openContainer, contents);
            ci.cancel();
        }
    }
}
