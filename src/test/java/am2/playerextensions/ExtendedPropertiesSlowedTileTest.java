package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEffectCodec;

public class ExtendedPropertiesSlowedTileTest {

    @Test
    public void slowedTileStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        TimedBlockEffect effect = new TimedBlockEffect(1, 2, 3, 4, 5);
        String raw = TimedEffectCodec.encodeSlowedTile(effect);

        properties.addToExtraVariables(raw, "10");
        assertEquals(
            Integer.valueOf(10),
            properties.getSlowedTiles()
                .get(effect));

        properties.setSlowedTileDuration(effect, 4);
        assertEquals(
            Integer.valueOf(4),
            properties.getSlowedTiles()
                .get(effect));
        assertEquals("4", properties.getExtraVariable(raw));

        properties.removeSlowedTile(effect);
        assertFalse(
            properties.getSlowedTiles()
                .containsKey(effect));
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void prefixRemovalAndClearDropSlowedTiles() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = TimedEffectCodec.encodeSlowedTile(new TimedBlockEffect(1, 2, 3, 4, 5));
        properties.addToExtraVariables(raw, "10");

        properties.removeExtraVariablesContaining("accelerated_slow_tile_");
        assertTrue(
            properties.getSlowedTiles()
                .isEmpty());

        properties.addToExtraVariables(raw, "10");
        properties.clearExtraVariables();
        assertTrue(
            properties.getSlowedTiles()
                .isEmpty());
    }

    @Test
    public void malformedDurationDoesNotCreateATypedEntry() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("accelerated_slow_tile_1_2_3_4_5", "bad");

        assertTrue(
            properties.getSlowedTiles()
                .isEmpty());
        assertTrue(properties.hasExtraVariable("accelerated_slow_tile_1_2_3_4_5"));
    }
}
