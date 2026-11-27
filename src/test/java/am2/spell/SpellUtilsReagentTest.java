package am2.spell;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

/**
 * Pins the corrupt-reagent-array guard. The old loop read {@code i + 1}/{@code i + 2} without checking that the
 * array length was a multiple of three.
 */
public class SpellUtilsReagentTest {

    @Test
    public void readsCompleteTriples() {
        ArrayList<int[]> triples = SpellUtils.readReagentTriples(new int[] { 1000, 2, 1 });

        assertEquals(1, triples.size());
        assertArrayEquals(new int[] { 1000, 2, 1 }, triples.get(0));
    }

    @Test
    public void ignoresATrailingIncompleteTriple() {
        ArrayList<int[]> triples = SpellUtils.readReagentTriples(new int[] { 1000, 2, 1, 5 });
        assertEquals(1, triples.size());
    }

    @Test
    public void handlesShortAndNullArrays() {
        assertTrue(
            SpellUtils.readReagentTriples(null)
                .isEmpty());
        assertTrue(
            SpellUtils.readReagentTriples(new int[0])
                .isEmpty());
        assertTrue(
            SpellUtils.readReagentTriples(new int[] { 1 })
                .isEmpty());
        assertTrue(
            SpellUtils.readReagentTriples(new int[] { 1, 2 })
                .isEmpty());
    }

    @Test
    public void unknownItemIdsAreSkipped() {
        assertTrue(
            SpellUtils.readReagents(new int[] { Integer.MAX_VALUE, 1, 0 })
                .isEmpty());
    }
}
