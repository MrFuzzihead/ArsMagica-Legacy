package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

/**
 * Pins the no-match fast path used by the per-tick effect scan: the old method always allocated a HashMap, even when
 * the player had no matching properties at all.
 */
public class ExtendedPropertiesExtraVariablesTest {

    @Test
    public void emptyPropertiesReturnTheSharedEmptyMap() {
        ExtendedProperties properties = new ExtendedProperties();

        assertSame(Collections.emptyMap(), properties.getExtraVariablesContains("custom_tile_"));
        assertSame(Collections.emptyMap(), properties.getExtraVariablesContains(null));
    }

    @Test
    public void noMatchingKeysReturnTheSharedEmptyMap() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("custom_tile_1_2_3_0_4", "10");

        assertSame(Collections.emptyMap(), properties.getExtraVariablesContains("spatialvortex_"));
        assertSame(Collections.emptyMap(), properties.getExtraVariablesContains("custom_slow_"));
    }

    @Test
    public void matchingKeysAreCopiedWithoutChangingTheSourceMap() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("custom_tile_1_2_3_0_4", "10");
        properties.addToExtraVariables("custom_tile_5_6_7_0_4", "20");
        properties.addToExtraVariables("custom_entity_uuid_4", "30");

        Map<String, String> tiles = properties.getExtraVariablesContains("custom_tile_");
        assertEquals(2, tiles.size());
        assertEquals("10", tiles.get("custom_tile_1_2_3_0_4"));
        assertEquals("20", tiles.get("custom_tile_5_6_7_0_4"));

        tiles.clear();
        assertTrue(properties.hasExtraVariable("custom_tile_1_2_3_0_4"));
        assertEquals(
            2,
            properties.getExtraVariablesContains("custom_tile_")
                .size());
    }
}
