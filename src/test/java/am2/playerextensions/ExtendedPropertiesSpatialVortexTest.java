package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import am2.playerextensions.effects.SpatialVortexEffect;
import am2.playerextensions.effects.TimedEffectCodec;

public class ExtendedPropertiesSpatialVortexTest {

    private static final SpatialVortexEffect EFFECT = new SpatialVortexEffect(1, 2, 3, 4);

    @Test
    public void spatialVortexStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = TimedEffectCodec.encodeSpatialVortex(EFFECT);

        properties.addToExtraVariables(raw, "AN");
        assertEquals(
            "AN",
            properties.getSpatialVortices()
                .get(EFFECT));

        properties.removeFromExtraVariables(raw);
        assertTrue(
            properties.getSpatialVortices()
                .isEmpty());
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void prefixRemovalAndClearDropSpatialVortices() {
        ExtendedProperties properties = new ExtendedProperties();
        String raw = TimedEffectCodec.encodeSpatialVortex(EFFECT);
        properties.addToExtraVariables(raw, "AN");

        properties.removeExtraVariablesContaining("spatialvortex_");
        assertTrue(
            properties.getSpatialVortices()
                .isEmpty());

        properties.addToExtraVariables(raw, "AN");
        properties.clearExtraVariables();
        assertTrue(
            properties.getSpatialVortices()
                .isEmpty());
    }

    @Test
    public void malformedKeyIsNotTyped() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("spatialvortex_a_b_c_d", "AN");

        assertTrue(
            properties.getSpatialVortices()
                .isEmpty());
        assertTrue(properties.hasExtraVariable("spatialvortex_a_b_c_d"));
    }
}
