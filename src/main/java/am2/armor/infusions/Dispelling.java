package am2.armor.infusions;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import am2.api.items.armor.IArmorImbuement;
import am2.api.items.armor.ImbuementApplicationTypes;
import am2.api.items.armor.ImbuementTiers;
import am2.mixins.early.PotionAccessor;

public class Dispelling implements IArmorImbuement {

    @Override
    public String getID() {
        return "dispel";
    }

    @Override
    public int getIconIndex() {
        return 25;
    }

    @Override
    public ImbuementTiers getTier() {
        return ImbuementTiers.FOURTH;
    }

    @Override
    public EnumSet<ImbuementApplicationTypes> getApplicationTypes() {
        return EnumSet.of(ImbuementApplicationTypes.ON_TICK);
    }

    @Override
    public boolean applyEffect(EntityPlayer player, World world, ItemStack stack, ImbuementApplicationTypes matchedType,
        Object... params) {
        if (player.getActivePotionEffects()
            .size() == 0) return false;

        if (player.worldObj.isRemote) return false;

        ArrayList<Integer> effectsToRemove = new ArrayList<Integer>();
        Object[] safeCopy = player.getActivePotionEffects()
            .toArray();
        for (Object o : safeCopy) {
            PotionEffect pe = (PotionEffect) o;
            Potion potionType = Potion.potionTypes[pe.getPotionID()];
            // Treat an unknown potion as a bad effect so it is always cleared rather than leaking a harmful buff.
            boolean badEffect = potionType == null || ((PotionAccessor) potionType).am2$isBadEffect();
            if (pe.getIsAmbient() || !badEffect) continue;
            effectsToRemove.add(pe.getPotionID());
        }

        for (Integer i : effectsToRemove) {
            player.removePotionEffect(i);
        }
        return effectsToRemove.size() > 0;
    }

    @Override
    public int[] getValidSlots() {
        return new int[] { ImbuementRegistry.SLOT_LEGS };
    }

    @Override
    public boolean canApplyOnCooldown() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 600;
    }

    @Override
    public int getArmorDamage() {
        return 75;
    }
}
