package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEffectCodec;

public class ExtendedPropertiesAcceleratedTileTest {

    @Test
    public void addUpdateAndRemoveKeepTheTypedMapAndLegacyStringInSync() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = "accelerated_fast_tile_1_2_3_4_5";
        TimedBlockEffect effect = TimedEffectCodec.parseAcceleratedTile(raw);

        properties.addToExtraVariables(raw, "10");

        assertEquals(
            Integer.valueOf(10),
            properties.getAcceleratedTiles()
                .get(effect));

        properties.setAcceleratedTileDuration(effect, 7);
        assertEquals(
            Integer.valueOf(7),
            properties.getAcceleratedTiles()
                .get(effect));
        assertEquals("7", properties.getExtraVariable(raw));

        properties.removeAcceleratedTile(effect);
        assertFalse(
            properties.getAcceleratedTiles()
                .containsKey(effect));
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void malformedDurationDoesNotCreateATypedEntry() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("accelerated_fast_tile_1_2_3_4_5", "not-a-number");

        assertTrue(
            properties.getAcceleratedTiles()
                .isEmpty());
        assertTrue(properties.hasExtraVariable("accelerated_fast_tile_1_2_3_4_5"));
    }

    @Test
    public void prefixRemovalAndClearDropTheTypedMap() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("accelerated_fast_tile_1_2_3_4_5", "10");
        properties.removeExtraVariablesContaining("accelerated_fast_tile_");
        assertTrue(
            properties.getAcceleratedTiles()
                .isEmpty());

        properties.addToExtraVariables("accelerated_fast_tile_1_2_3_4_5", "10");
        properties.clearExtraVariables();
        assertTrue(
            properties.getAcceleratedTiles()
                .isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rebuildRepopulatesTheTypedMapFromRawProperties() throws Exception {
        ExtendedProperties properties = new ExtendedProperties();
        Field extra = ExtendedProperties.class.getDeclaredField("extra_properties");
        extra.setAccessible(true);
        ((Map<String, String>) extra.get(properties)).put("accelerated_fast_tile_1_2_3_4_5", "10");

        Method rebuild = ExtendedProperties.class.getDeclaredMethod("rebuildTimedEffectKeys");
        rebuild.setAccessible(true);
        rebuild.invoke(properties);

        TimedBlockEffect effect = TimedEffectCodec.parseAcceleratedTile("accelerated_fast_tile_1_2_3_4_5");
        assertEquals(
            Integer.valueOf(10),
            properties.getAcceleratedTiles()
                .get(effect));
    }
}
