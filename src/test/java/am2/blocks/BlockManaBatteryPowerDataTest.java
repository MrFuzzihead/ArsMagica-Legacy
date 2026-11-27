package am2.blocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import am2.api.power.PowerTypes;
import am2.blocks.tileentities.TileEntityManaBattery;
import am2.power.PowerNodeRegistry;

public class BlockManaBatteryPowerDataTest {

    @Test
    public void savePowerDataKeepsEveryStoredType() {
        PowerNodeRegistry registry = mock(PowerNodeRegistry.class);
        TileEntityManaBattery battery = new TileEntityManaBattery();
        battery.setPowerType(PowerTypes.LIGHT, false);
        when(registry.getPower(battery, PowerTypes.LIGHT)).thenReturn(123.0F);
        when(registry.getPower(battery, PowerTypes.DARK)).thenReturn(50.0F);

        NBTTagCompound tag = BlockManaBattery.savePowerData(registry, battery);
        NBTTagList powers = tag.getTagList("mana_battery_powers", 10);

        assertEquals(2, powers.tagCount());
        assertEquals(123.0F, tag.getFloat("mana_battery_charge"), 0.001F);
        assertTrue(BlockManaBattery.hasStoredPower(registry, battery));
    }

    @Test
    public void loadPowerDataRestoresEveryStoredType() {
        PowerNodeRegistry source = mock(PowerNodeRegistry.class);
        PowerNodeRegistry target = mock(PowerNodeRegistry.class);
        TileEntityManaBattery battery = new TileEntityManaBattery();
        when(source.getPower(battery, PowerTypes.LIGHT)).thenReturn(123.0F);
        when(source.getPower(battery, PowerTypes.DARK)).thenReturn(50.0F);
        NBTTagCompound tag = BlockManaBattery.savePowerData(source, battery);

        BlockManaBattery.loadPowerData(target, battery, tag);

        verify(target).setPower(battery, PowerTypes.LIGHT, 123.0F);
        verify(target).setPower(battery, PowerTypes.DARK, 50.0F);
    }

    @Test
    public void legacySingleTypeTagStillLoads() {
        PowerNodeRegistry registry = mock(PowerNodeRegistry.class);
        TileEntityManaBattery battery = new TileEntityManaBattery();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("mana_battery_charge", 42.0F);
        tag.setInteger("mana_battery_powertype", PowerTypes.LIGHT.ID());

        BlockManaBattery.loadPowerData(registry, battery, tag);

        verify(registry).setPower(battery, PowerTypes.LIGHT, 42.0F);
        assertFalse(BlockManaBattery.hasStoredPower(registry, battery));
    }
}
