package am2.enchantments;

import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import am2.api.enchantment.IAMEnchantmentHelper;

public class AMEnchantmentHelper implements IAMEnchantmentHelper {

    @Override
    public int getSoulboundID() {
        return AMEnchantments.soulbound.effectId;
    }

    @Override
    public int getMagicResistID() {
        return AMEnchantments.magicResist.effectId;
    }

    public static ItemStack soulbindStack(ItemStack stack) {
        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        map.put(AMEnchantments.soulbound.effectId, 1);
        EnchantmentHelper.setEnchantments(map, stack);
        return stack;
    }

    public static ItemStack silkTouchStack(ItemStack stack, int level) {
        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        if (level > 0) {
            map.put(Enchantment.silkTouch.effectId, level);
        } else {
            map.remove(Enchantment.silkTouch.effectId);
        }
        EnchantmentHelper.setEnchantments(map, stack);
        return stack;
    }

    public static ItemStack fortuneStack(ItemStack stack, int level) {
        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        applyEnchantment(map, Enchantment.fortune.effectId, level);
        EnchantmentHelper.setEnchantments(map, stack);
        return stack;
    }

    public static ItemStack lootingStack(ItemStack stack, int level) {
        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(stack);
        applyEnchantment(map, Enchantment.looting.effectId, level);
        EnchantmentHelper.setEnchantments(map, stack);
        return stack;
    }

    /**
     * Sets or removes exactly the requested enchantment. The old fortune/looting zero-level paths removed Silk Touch
     * instead, leaving the intended enchantment in place and silently stripping an unrelated one.
     */
    static void applyEnchantment(Map<Integer, Integer> map, int enchantmentId, int level) {
        if (level > 0) {
            map.put(enchantmentId, level);
        } else {
            map.remove(enchantmentId);
        }
    }

    public static void copyEnchantments(ItemStack source, ItemStack dest) {
        Map<Integer, Integer> map = EnchantmentHelper.getEnchantments(source);
        if (map != null) {
            EnchantmentHelper.setEnchantments(map, dest);
        }
    }
}
