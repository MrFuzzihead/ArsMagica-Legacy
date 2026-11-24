package am2.items;

import static org.junit.Assert.assertEquals;

import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;

import org.junit.Test;

/**
 * Pins the soulspike override that replaced the global {@code Item.setDamage} coremod patch.
 */
public class ItemSoulspikeTest {

    @Test
    public void ordinaryDurabilityChangesAreIgnored() {
        ItemSoulspike spike = new ItemSoulspike(ToolMaterial.EMERALD);
        ItemStack stack = new ItemStack(spike);

        spike.setDamage(stack, 5);
        assertEquals(0, stack.getItemDamage());
    }

    @Test
    public void sentinelValuesAreApplied() {
        ItemSoulspike spike = new ItemSoulspike(ToolMaterial.EMERALD);
        ItemStack stack = new ItemStack(spike);

        spike.setDamage(stack, 66);
        assertEquals(66, stack.getItemDamage());

        spike.setDamage(stack, 0);
        assertEquals(0, stack.getItemDamage());
    }
}
