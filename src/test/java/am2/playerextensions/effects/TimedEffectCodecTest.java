package am2.playerextensions.effects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Test;

public class TimedEffectCodecTest {

    @Test
    public void acceleratedTileRoundTripsCoordinatesAndPower() {
        TimedBlockEffect effect = TimedEffectCodec.parseAcceleratedTile("accelerated_fast_tile_10_64_-3_0_4");

        assertEquals(new TimedBlockEffect(10, 64, -3, 0, 4), effect);
        assertEquals("10_64_-3_0", effect.representation());
    }

    @Test
    public void timedBlockIgnoresPowerAndBuildsRepresentation() {
        TimedBlockEffect effect = TimedEffectCodec.parseTimedBlock("shielded_tile_10_64_-3_7");

        assertEquals(10, effect.x);
        assertEquals(64, effect.y);
        assertEquals(-3, effect.z);
        assertEquals(7, effect.dimension);
        assertEquals(0, effect.power);
        assertEquals("10_64_-3_7", effect.representation());
    }

    @Test
    public void entityRoundTripsUuidAndPower() {
        UUID uuid = UUID.randomUUID();
        TimedEntityEffect effect = TimedEffectCodec.parseEntity("accelerated_fast_entity_3_" + uuid);

        assertEquals(uuid, effect.entityUUID);
        assertEquals(3, effect.power);
    }

    @Test
    public void spatialVortexRoundTripsCoordinates() {
        SpatialVortexEffect effect = TimedEffectCodec.parseSpatialVortex("spatialvortex_10_64_-3_7");

        assertEquals(10, effect.x);
        assertEquals(64, effect.y);
        assertEquals(-3, effect.z);
        assertEquals(7, effect.dimension);
    }

    @Test
    public void malformedKeysAreRejected() {
        assertNull(TimedEffectCodec.parseAcceleratedTile(null));
        assertNull(TimedEffectCodec.parseAcceleratedTile("accelerated_fast_tile_1_2"));
        assertNull(TimedEffectCodec.parseTimedBlock("shielded_tile_1_2"));
        assertNull(TimedEffectCodec.parseEntity("accelerated_fast_entity_3_not-a-uuid"));
        assertNull(TimedEffectCodec.parseSpatialVortex("spatialvortex_a_b_c_d"));
    }

    @Test
    public void typedKeysUseValueEquality() {
        assertEquals(new TimedBlockEffect(1, 2, 3, 4, 5), new TimedBlockEffect(1, 2, 3, 4, 5));
        assertNotEquals(new TimedBlockEffect(1, 2, 3, 4, 5), new TimedBlockEffect(1, 2, 3, 4, 6));
        assertEquals(new SpatialVortexEffect(1, 2, 3, 4), new SpatialVortexEffect(1, 2, 3, 4));
    }
}
