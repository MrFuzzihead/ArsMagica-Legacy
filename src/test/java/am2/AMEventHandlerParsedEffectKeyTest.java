package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Parsers for the remaining timed-effect key formats. Malformed legacy keys must be rejected rather than throwing from
 * the per-tick bookkeeping.
 */
public class AMEventHandlerParsedEffectKeyTest {

    @Before
    public void clearCaches() {
        AMEventHandler.clearParsedEffectKeyCaches();
    }

    @After
    public void clearCachesAfter() {
        AMEventHandler.clearParsedEffectKeyCaches();
    }

    @Test
    public void acceleratedEntityKeyParsesPowerAndUuid() {
        UUID uuid = UUID.randomUUID();
        AMEventHandler.AcceleratedEntityKey key = AMEventHandler
            .acceleratedEntityKey("accelerated_fast_entity_3_" + uuid);

        assertEquals(3, key.power);
        assertEquals(uuid, key.entityUUID);
        assertSame(key, AMEventHandler.acceleratedEntityKey("accelerated_fast_entity_3_" + uuid));
    }

    @Test
    public void malformedEntityKeysAreRejected() {
        assertNull(AMEventHandler.acceleratedEntityKey(null));
        assertNull(AMEventHandler.acceleratedEntityKey("accelerated_fast_entity_3"));
        assertNull(AMEventHandler.acceleratedEntityKey("accelerated_fast_entity_x_" + UUID.randomUUID()));
        assertNull(AMEventHandler.acceleratedEntityKey("accelerated_fast_entity_3_not-a-uuid"));
    }

    @Test
    public void spatialVortexKeyParsesCoordinatesAndDimension() {
        AMEventHandler.SpatialVortexKey key = AMEventHandler.spatialVortexKey("spatialvortex_10_64_-3_7");

        assertEquals(10, key.x);
        assertEquals(64, key.y);
        assertEquals(-3, key.z);
        assertEquals(7, key.dimension);
        assertSame(key, AMEventHandler.spatialVortexKey("spatialvortex_10_64_-3_7"));
    }

    @Test
    public void malformedSpatialVortexKeysAreRejected() {
        assertNull(AMEventHandler.spatialVortexKey(null));
        assertNull(AMEventHandler.spatialVortexKey("spatialvortex_10_64"));
        assertNull(AMEventHandler.spatialVortexKey("spatialvortex_a_b_c_d"));
    }

    @Test
    public void timedBlockKeyParsesCoordinatesAndBuildsTheShieldKey() {
        AMEventHandler.TimedBlockKey key = AMEventHandler.timedBlockKey("shielded_tile_10_64_-3_7");

        assertEquals(10, key.x);
        assertEquals(64, key.y);
        assertEquals(-3, key.z);
        assertEquals(7, key.dimension);
        assertEquals("10_64_-3_7", key.representation());
        assertSame(key, AMEventHandler.timedBlockKey("shielded_tile_10_64_-3_7"));
    }

    @Test
    public void malformedTimedBlockKeysAreRejected() {
        assertNull(AMEventHandler.timedBlockKey(null));
        assertNull(AMEventHandler.timedBlockKey("timefortified_tile_10_64"));
        assertNull(AMEventHandler.timedBlockKey("timefortified_tile_a_b_c_d"));
    }

    @Test
    public void clearingCachesDropsParsedInstances() {
        String raw = "spatialvortex_1_2_3_4";
        AMEventHandler.SpatialVortexKey first = AMEventHandler.spatialVortexKey(raw);
        AMEventHandler.clearParsedEffectKeyCaches();

        assertTrue(first != null);
        assertTrue(first != AMEventHandler.spatialVortexKey(raw));
    }
}
