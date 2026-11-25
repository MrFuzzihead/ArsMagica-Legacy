package am2.playerextensions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;

/**
 * Pins the timed-effect index used to skip {@code onEntityLiving}'s per-entity timed bookkeeping when a player has no
 * timed effects at all.
 */
public class ExtendedPropertiesTimedEffectsTest {

    private static final String[] PREFIXES = { "accelerated_fast_tile_", "accelerated_fast_entity_",
        "accelerated_slow_tile_", "accelerated_slow_entity_", "spatialvortex_", "timefortified_tile_",
        "shielded_tile_" };

    @Test
    public void nonTimedPropertiesDoNotEnableTimedEffects() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("ethereal", "10");

        assertFalse(properties.hasTimedEffects());
    }

    @Test
    public void everyTimedEffectPrefixIsTracked() {
        for (String prefix : PREFIXES) {
            ExtendedProperties properties = new ExtendedProperties();
            properties.addToExtraVariables(prefix + "1_2_3_4", "10");
            assertTrue(prefix + " must be tracked", properties.hasTimedEffects());
            properties.removeFromExtraVariables(prefix + "1_2_3_4");
            assertFalse(prefix + " must be untracked", properties.hasTimedEffects());
        }
    }

    @Test
    public void removingOneEffectDoesNotClearAnother() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("accelerated_fast_tile_1_2_3_4_5", "10");
        properties.addToExtraVariables("shielded_tile_1_2_3_4", "10");

        properties.removeFromExtraVariables("accelerated_fast_tile_1_2_3_4_5");
        assertTrue(properties.hasTimedEffects());

        properties.removeFromExtraVariables("shielded_tile_1_2_3_4");
        assertFalse(properties.hasTimedEffects());
    }

    @Test
    public void removingByPrefixUpdatesTheIndex() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("accelerated_fast_tile_1_2_3_4_5", "10");
        properties.addToExtraVariables("accelerated_slow_entity_4_" + java.util.UUID.randomUUID(), "10");
        properties.addToExtraVariables("spatialvortex_1_2_3_4", "10");

        properties.removeExtraVariablesContaining("accelerated_");
        assertTrue(properties.hasTimedEffects());

        properties.removeExtraVariablesContaining("spatialvortex_");
        assertFalse(properties.hasTimedEffects());
    }

    @Test
    public void clearDropsTheIndex() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("shielded_tile_1_2_3_4", "10");

        properties.clearExtraVariables();

        assertFalse(properties.hasTimedEffects());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void rebuildsIndexFromRawProperties() throws Exception {
        ExtendedProperties properties = new ExtendedProperties();
        Field extra = ExtendedProperties.class.getDeclaredField("extra_properties");
        extra.setAccessible(true);
        ((Map<String, String>) extra.get(properties)).put("timefortified_tile_1_2_3_4", "10");

        Method rebuild = ExtendedProperties.class.getDeclaredMethod("rebuildTimedEffectKeys");
        rebuild.setAccessible(true);
        rebuild.invoke(properties);

        assertTrue(properties.hasTimedEffects());
    }
}
