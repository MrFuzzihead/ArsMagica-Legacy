package am2.lore;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;

/**
 * Covers the supplier-based compendium entry factory that replaced reflective no-arg construction.
 */
public class CompendiumEntryTypeTest {

    @Test
    public void createBlankUsesTheRegisteredFactory() {
        CompendiumEntry entry = mock(CompendiumEntry.class);
        CompendiumEntryType type = new CompendiumEntryType("category", "node", "Label", 0, () -> entry);

        assertSame(entry, type.createBlank());
    }

    @Test
    public void createBlankCanReturnNullForAnUnavailableType() {
        CompendiumEntryType type = new CompendiumEntryType("category", "node", "Label", 0, () -> null);
        assertNull(type.createBlank());
    }
}
