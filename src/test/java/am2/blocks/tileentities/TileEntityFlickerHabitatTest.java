package am2.blocks.tileentities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import am2.api.math.AMVector3;

/**
 * Pins the out-marker removal: the old code removed an Integer key from a map whose values were lists, so breaking an
 * output crystal marker left a stale location behind.
 */
public class TileEntityFlickerHabitatTest {

    @Test
    public void removeOutMarkerLocationRemovesTheVectorFromEveryPriorityList() {
        TileEntityFlickerHabitat habitat = new TileEntityFlickerHabitat();
        AMVector3 marker = new AMVector3(1.0D, 2.0D, 3.0D);
        habitat.switchMarkerPriority(marker, 0, 1);

        assertEquals(1, habitat.getOutListSize(1));
        habitat.removeOutMarkerLocation(1, 2, 3);
        assertEquals(0, habitat.getOutListSize(1));
    }
}
