package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;

import org.junit.Test;

/**
 * Pins the wildcard-meta contract: a wildcard key equals a specific key, so their hashes must match.
 */
public class CompoundKeyTest {

    @Test
    public void wildcardMetaEqualsSpecificMetaWithTheSameHash() {
        Item item = new Item();
        CompoundKey specific = new CompoundKey(item, 3);
        CompoundKey wildcard = new CompoundKey(item, -1);

        assertTrue(specific.equals(wildcard));
        assertTrue(wildcard.equals(specific));
        assertEquals(specific.hashCode(), wildcard.hashCode());
    }

    @Test
    public void differentItemsAreNotEqual() {
        CompoundKey a = new CompoundKey(new Item(), 0);
        CompoundKey b = new CompoundKey(new Item(), 0);
        assertFalse(a.equals(b));
    }
}
