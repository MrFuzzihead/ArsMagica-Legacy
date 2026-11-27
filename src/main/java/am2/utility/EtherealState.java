package am2.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import am2.items.ItemSoulspike;

/**
 * Normal-code home for the "ethereal player" check.
 *
 * <p>
 * This used to live on {@code MysteriumPatchesFixesMagicka} and was called by the ASM {@code @Fix} methods and by
 * several event handlers. Moving it here lets the replacement Mixins and the remaining normal code share one
 * implementation while the old coremod class is retired.
 * </p>
 */
public final class EtherealState {

    private EtherealState() {}

    public static boolean isEthereal(EntityPlayer player) {
        if (player == null || player.inventory == null) {
            return false;
        }

        ItemStack boots = player.inventory.armorInventory[0];
        return boots != null && ItemSoulspike.bootsHaveEtherealTag(boots);
    }
}
