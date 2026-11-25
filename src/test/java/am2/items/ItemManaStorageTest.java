package am2.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/** Mana storage must never expose or persist a negative amount, even with corrupt/legacy NBT. */
public class ItemManaStorageTest {

    @Test
    public void manaStoneReadsNegativeNbtAsEmpty() {
        ItemStack stone = new ItemStack(new ItemManaStone());
        stone.setTagCompound(new NBTTagCompound());
        stone.stackTagCompound.setInteger("Stored_MANA", -50);

        assertEquals(0, ItemManaStone.getManaInStone(stone));
        assertFalse(ItemManaStone.isFull(stone));
    }

    @Test
    public void manaStoneAddingToCorruptNbtRecoversFromEmpty() {
        ItemStack stone = new ItemStack(new ItemManaStone());
        stone.setTagCompound(new NBTTagCompound());
        stone.stackTagCompound.setInteger("Stored_MANA", -50);

        ItemManaStone.addManaToStone(stone, 10);

        assertEquals(10, ItemManaStone.getManaInStone(stone));
    }

    @Test
    public void manaStoneDeductionsClampAtZero() {
        ItemStack stone = new ItemStack(new ItemManaStone());
        ItemManaStone.addManaToStone(stone, 5);

        ItemManaStone.deductManaFromStone(stone, 10);

        assertEquals(0, ItemManaStone.getManaInStone(stone));
        assertEquals(0, stone.stackTagCompound.getInteger("Stored_MANA"));
    }

    @Test
    public void manaStoneStillCapsAtItsMaximum() {
        ItemStack stone = new ItemStack(new ItemManaStone());
        ItemManaStone.addManaToStone(stone, 5000);

        assertEquals(1000, ItemManaStone.getManaInStone(stone));
    }

    @Test
    public void soulspikeReadsNegativeNbtAsEmpty() {
        ItemStack spike = new ItemStack(new ItemSoulspike(ToolMaterial.EMERALD));
        spike.setTagCompound(new NBTTagCompound());
        spike.stackTagCompound.setInteger("Stored_GREATERMANA", -50);

        assertEquals(0, ItemSoulspike.getManaInSpike(spike));
        assertFalse(ItemSoulspike.isFull(spike));
    }

    @Test
    public void soulspikeAddingToCorruptNbtRecoversFromEmpty() {
        ItemStack spike = new ItemStack(new ItemSoulspike(ToolMaterial.EMERALD));
        spike.setTagCompound(new NBTTagCompound());
        spike.stackTagCompound.setInteger("Stored_GREATERMANA", -50);

        ItemSoulspike.addManaToSpike(spike, 10);

        assertEquals(10, ItemSoulspike.getManaInSpike(spike));
    }

    @Test
    public void soulspikeDeductionsClampAtZero() {
        ItemStack spike = new ItemStack(new ItemSoulspike(ToolMaterial.EMERALD));
        ItemSoulspike.addManaToSpike(spike, 5);

        ItemSoulspike.deductManaFromSpike(spike, 10);

        assertEquals(0, ItemSoulspike.getManaInSpike(spike));
        assertEquals(0, spike.stackTagCompound.getInteger("Stored_GREATERMANA"));
    }

    @Test
    public void soulspikeStillCapsAtItsMaximum() {
        ItemStack spike = new ItemStack(new ItemSoulspike(ToolMaterial.EMERALD));
        ItemSoulspike.addManaToSpike(spike, 500000);

        assertEquals(150000, ItemSoulspike.getManaInSpike(spike));
    }
}
