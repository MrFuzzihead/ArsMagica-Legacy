package am2.playerextensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import am2.playerextensions.effects.TimedEffectCodec;
import am2.playerextensions.effects.TimedEntityEffect;

public class ExtendedPropertiesEntityEffectTest {

    @Test
    public void acceleratedEntityStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        UUID uuid = UUID.randomUUID();
        String raw = TimedEffectCodec.encodeAcceleratedEntity(new TimedEntityEffect(uuid, 4));
        TimedEntityEffect effect = TimedEffectCodec.parseEntity(raw);

        properties.addToExtraVariables(raw, "10");
        assertEquals(
            Integer.valueOf(10),
            properties.getAcceleratedEntities()
                .get(effect));

        properties.setAcceleratedEntityDuration(effect, 5);
        assertEquals(
            Integer.valueOf(5),
            properties.getAcceleratedEntities()
                .get(effect));

        properties.removeAcceleratedEntity(effect);
        assertFalse(
            properties.getAcceleratedEntities()
                .containsKey(effect));
        assertFalse(properties.hasExtraVariable(raw));
    }

    @Test
    public void slowedEntityStorageTracksBothRepresentations() {
        ExtendedProperties properties = new ExtendedProperties();
        UUID uuid = UUID.randomUUID();
        String raw = TimedEffectCodec.encodeSlowedEntity(new TimedEntityEffect(uuid, 3));
        TimedEntityEffect effect = TimedEffectCodec.parseEntity(raw);

        properties.addToExtraVariables(raw, "8");
        assertEquals(
            Integer.valueOf(8),
            properties.getSlowedEntities()
                .get(effect));

        properties.setSlowedEntityDuration(effect, 3);

        properties.removeSlowedEntity(effect);
        assertTrue(
            properties.getSlowedEntities()
                .isEmpty());
    }

    @Test
    public void prefixRemovalDropsBothEntityMaps() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables(
            TimedEffectCodec.encodeAcceleratedEntity(new TimedEntityEffect(UUID.randomUUID(), 2)),
            "10");
        properties.addToExtraVariables(
            TimedEffectCodec.encodeSlowedEntity(new TimedEntityEffect(UUID.randomUUID(), 2)),
            "10");

        properties.removeExtraVariablesContaining("accelerated_fast_entity_");
        assertTrue(
            properties.getAcceleratedEntities()
                .isEmpty());
        assertFalse(
            properties.getSlowedEntities()
                .isEmpty());

        properties.removeExtraVariablesContaining("accelerated_slow_entity_");
        assertTrue(
            properties.getSlowedEntities()
                .isEmpty());
    }
}
