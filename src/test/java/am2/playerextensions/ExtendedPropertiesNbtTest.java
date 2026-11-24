package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

/**
 * Pins the NBT index handling that the old save/load code got wrong: compendium entries shared the extra-properties
 * index counter, so they were written at index N.. but read from index 0...
 */
public class ExtendedPropertiesNbtTest {

    @Test
    public void missingContingencyStackIsRestoredToSnowball() throws Exception {
        ExtendedProperties properties = new ExtendedProperties();
        Field field = ExtendedProperties.class.getDeclaredField("contingencyStacks");
        field.setAccessible(true);
        ((ItemStack[]) field.get(properties))[0] = null;

        ItemStack effect = properties.getContingencyEffect(0);
        assertNotNull(effect);
        assertEquals(Items.snowball, effect.getItem());
    }

    @Test
    public void savingWithANullContingencyDoesNotThrow() throws Exception {
        ExtendedProperties properties = new ExtendedProperties();
        Field field = ExtendedProperties.class.getDeclaredField("contingencyStacks");
        field.setAccessible(true);
        ((ItemStack[]) field.get(properties))[0] = null;

        properties.saveNBTData(new NBTTagCompound());
    }

    @Test
    public void compendiumEntriesAreWrittenFromIndexZero() {
        Map<String, String> extras = new LinkedHashMap<String, String>();
        extras.put("ethereal", "100");
        extras.put("karma", "1");
        Map<String, String> compendium = new LinkedHashMap<String, String>();
        compendium.put("entry_a", "U");
        compendium.put("entry_b", "N");

        NBTTagCompound tag = new NBTTagCompound();
        ExtendedProperties.writeStringMap(tag, "persistentobj", "persistentobjname", "persistentobjsize", extras);
        ExtendedProperties.writeStringMap(tag, "compentry", "compentryname", "compendiumsize", compendium);

        assertEquals("entry_a", tag.getString("compentryname0"));
        assertEquals("U", tag.getString("compentry0"));
        assertEquals("entry_b", tag.getString("compentryname1"));
        assertEquals("N", tag.getString("compentry1"));
        assertEquals(2, tag.getInteger("compendiumsize"));
    }

    @Test
    public void bothMapsRoundTripWhenExtrasAreNonEmpty() {
        Map<String, String> extras = new LinkedHashMap<String, String>();
        extras.put("ethereal", "100");
        extras.put("karma", "1");
        Map<String, String> compendium = new LinkedHashMap<String, String>();
        compendium.put("entry_a", "U");
        compendium.put("entry_b", "N");

        NBTTagCompound tag = new NBTTagCompound();
        ExtendedProperties.writeStringMap(tag, "persistentobj", "persistentobjname", "persistentobjsize", extras);
        ExtendedProperties.writeStringMap(tag, "compentry", "compentryname", "compendiumsize", compendium);

        Map<String, String> loadedExtras = new LinkedHashMap<String, String>();
        Map<String, String> loadedCompendium = new LinkedHashMap<String, String>();
        ExtendedProperties.readStringMap(tag, "persistentobj", "persistentobjname", "persistentobjsize", loadedExtras);
        ExtendedProperties.readStringMap(tag, "compentry", "compentryname", "compendiumsize", loadedCompendium);

        assertEquals(extras, loadedExtras);
        assertEquals(compendium, loadedCompendium);
    }

    @Test
    public void compendiumRoundTripsWhenExtrasAreEmpty() {
        Map<String, String> compendium = new LinkedHashMap<String, String>();
        compendium.put("entry_a", "U");

        NBTTagCompound tag = new NBTTagCompound();
        ExtendedProperties
            .writeStringMap(tag, "persistentobj", "persistentobjname", "persistentobjsize", Collections.emptyMap());
        ExtendedProperties.writeStringMap(tag, "compentry", "compentryname", "compendiumsize", compendium);

        Map<String, String> loaded = new LinkedHashMap<String, String>();
        ExtendedProperties.readStringMap(tag, "compentry", "compentryname", "compendiumsize", loaded);
        assertEquals(compendium, loaded);
    }
}
