package am2;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class AMKeyBindingsTest {

    @Test
    public void spellBookAndAffinityKeysDoNotCollide() {
        assertNotEquals(
            AMKeyBindings.SpellBookNextSpellKey.getKeyCode(),
            AMKeyBindings.AffinityActivationKey.getKeyCode());
    }
}
