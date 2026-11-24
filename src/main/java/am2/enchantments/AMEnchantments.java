package am2.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import am2.AMCore;
import am2.LogHelper;
import cpw.mods.fml.common.registry.LanguageRegistry;

public class AMEnchantments {

    public static EnchantMagicResist magicResist = registerEnchantment(EnchantMagicResist::new, 5, "magic_resist", 100);
    public static EnchantmentSoulbound soulbound = registerEnchantment(EnchantmentSoulbound::new, 0, "soulbound", 101);

    public void Init() {
        int defMR = AMCore.config.getConfigurableEnchantmentID("magic_resist", 100);

        Enchantment.addToBookList(magicResist);
        // Enchantment.addToBookList(soulbound); // no soulbound book! Bad!

        LanguageRegistry.instance()
            .addStringLocalization("enchantment.magicresist", "en_US", "Magic Resistance");
        LanguageRegistry.instance()
            .addStringLocalization("enchantment.soulbound", "en_US", "Soulbound");
    }

    @FunctionalInterface
    private interface EnchantmentFactory<T extends Enchantment> {

        T create(int id, int weight);
    }

    private static <T extends Enchantment> T registerEnchantment(EnchantmentFactory<T> factory, int weight,
        String configID, int default_value) {
        int start_value = AMCore.config.getConfigurableEnchantmentID(configID, default_value);
        int enchID = start_value;
        boolean fullcircle = true;
        do {
            if (Enchantment.enchantmentsList[enchID] == null) {
                fullcircle = false;
                break;
            }
            enchID++;
            enchID %= Enchantment.enchantmentsList.length;
        } while (enchID != start_value);

        if (fullcircle) {
            throw new ArrayIndexOutOfBoundsException("All enchantment IDs are in use...can't find a free one to take!");
        }

        LogHelper.info(
            "Attempting to set enchantment %s to ID %d (configured currently as %d)",
            configID,
            enchID,
            start_value);
        AMCore.config.updateConfigurableEnchantmentID(configID, enchID);

        try {
            T ench = factory.create(enchID, weight);
            LogHelper.info("Successfully registered enchanment!");
            return ench;
        } catch (Throwable t) {
            LogHelper.error("Failed to register enchantment %s!", configID);
            t.printStackTrace();
        }
        return null;
    }

    public static int GetEnchantmentLevelSpecial(int enchID, ItemStack stack) {
        int baseEnchLvl = EnchantmentHelper.getEnchantmentLevel(enchID, stack);
        /*
         * if (enchID == imbuedArmor.effectId || enchID == imbuedBow.effectId || enchID == imbuedWeapon.effectId){
         * if (baseEnchLvl > 3)
         * return (baseEnchLvl & 0x6000) >> 13;
         * }
         */
        return baseEnchLvl;
    }
}
