package am2.enchantments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;

import org.junit.Test;

/**
 * Pins the enchantment add/remove helper. The zero-level fortune/looting paths used to remove Silk Touch instead,
 * leaving the intended enchantment stuck and stripping an unrelated one.
 */
public class AMEnchantmentHelperTest {

    @Test
    public void removingFortuneKeepsSilkTouch() {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(Enchantment.silkTouch.effectId, 1);
        map.put(Enchantment.fortune.effectId, 2);

        AMEnchantmentHelper.applyEnchantment(map, Enchantment.fortune.effectId, 0);

        assertFalse(map.containsKey(Enchantment.fortune.effectId));
        assertTrue(map.containsKey(Enchantment.silkTouch.effectId));
    }

    @Test
    public void removingLootingKeepsSilkTouch() {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(Enchantment.silkTouch.effectId, 1);
        map.put(Enchantment.looting.effectId, 3);

        AMEnchantmentHelper.applyEnchantment(map, Enchantment.looting.effectId, 0);

        assertFalse(map.containsKey(Enchantment.looting.effectId));
        assertTrue(map.containsKey(Enchantment.silkTouch.effectId));
    }

    @Test
    public void settingALevelPutsThatEnchantment() {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();

        AMEnchantmentHelper.applyEnchantment(map, Enchantment.fortune.effectId, 4);

        assertEquals(Integer.valueOf(4), map.get(Enchantment.fortune.effectId));
    }
}
