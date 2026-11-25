package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEffectCodec;

public class ExtendedPropertiesFortifiedShieldTest {

    private static final TimedBlockEffect EFFECT = new TimedBlockEffect(1, 2, 3, 4, 0);

    @Test
    public void fortifiedTileStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = TimedEffectCodec.encodeFortifiedTile(EFFECT);

        properties.addToExtraVariables(raw, "10");
        assertEquals(
            Integer.valueOf(10),
            properties.getFortifiedTiles()
                .get(EFFECT));

        properties.setFortifiedTileDuration(EFFECT, 5);
        assertEquals("5", properties.getExtraVariable(raw));

        properties.removeFortifiedTile(EFFECT);
        assertTrue(
            properties.getFortifiedTiles()
                .isEmpty());
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void shieldedTileStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = TimedEffectCodec.encodeShieldedTile(EFFECT);

        properties.addToExtraVariables(raw, "10");
        assertEquals(
            Integer.valueOf(10),
            properties.getShieldedTiles()
                .get(EFFECT));

        properties.setShieldedTileDuration(EFFECT, 5);
        assertEquals("5", properties.getExtraVariable(raw));

        properties.removeShieldedTile(EFFECT);
        assertTrue(
            properties.getShieldedTiles()
                .isEmpty());
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void prefixRemovalAndClearDropBothMaps() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables(TimedEffectCodec.encodeFortifiedTile(EFFECT), "10");
        properties.addToExtraVariables(TimedEffectCodec.encodeShieldedTile(EFFECT), "10");

        properties.removeExtraVariablesContaining("timefortified_tile_");
        assertTrue(
            properties.getFortifiedTiles()
                .isEmpty());
        assertFalse(
            properties.getShieldedTiles()
                .isEmpty());

        properties.removeExtraVariablesContaining("shielded_tile_");
        assertTrue(
            properties.getShieldedTiles()
                .isEmpty());

        properties.addToExtraVariables(TimedEffectCodec.encodeFortifiedTile(EFFECT), "10");
        properties.clearExtraVariables();
        assertTrue(
            properties.getFortifiedTiles()
                .isEmpty());
    }
}
