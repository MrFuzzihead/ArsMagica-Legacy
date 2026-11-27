package am2.entities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntitySpellEffectSoundTest {

    @Test
    public void loopSoundIsThrottledToOncePerSecond() {
        assertFalse(EntitySpellEffect.shouldPlayLoopSound(0));
        assertFalse(EntitySpellEffect.shouldPlayLoopSound(19));
        assertTrue(EntitySpellEffect.shouldPlayLoopSound(20));
        assertFalse(EntitySpellEffect.shouldPlayLoopSound(21));
        assertTrue(EntitySpellEffect.shouldPlayLoopSound(40));
    }
}
