package am2.items;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

/**
 * Staff "damage" is charge, so the crafting-grid durability merge must not handle two staffs; the dynamic recipe does.
 */
public class ItemSpellStaffTest {

    @Test
    public void staffsAreNotGridRepairable() {
        assertFalse(new ItemSpellStaff(300, 0).isRepairable());
        assertFalse(new ItemSpellStaff(0, -1).isRepairable());
    }
}
