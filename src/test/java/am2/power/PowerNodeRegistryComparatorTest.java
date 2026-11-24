package am2.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import net.minecraft.world.ChunkCoordIntPair;

import org.junit.Test;

/**
 * Pins the lexicographic chunk comparator used by the power-node registry. The old comparator was not
 * antisymmetric, so a TreeMap could fail to find keys that were definitely inserted.
 */
public class PowerNodeRegistryComparatorTest {

    private final PowerNodeRegistry.ChunkCoordComparator comparator = new PowerNodeRegistry.ChunkCoordComparator();

    @Test
    public void comparatorIsAntisymmetric() {
        ChunkCoordIntPair a = new ChunkCoordIntPair(1, 5);
        ChunkCoordIntPair b = new ChunkCoordIntPair(2, 0);

        assertTrue(comparator.compare(a, b) < 0);
        assertTrue(comparator.compare(b, a) > 0);
        assertEquals(0, comparator.compare(a, new ChunkCoordIntPair(1, 5)));
    }

    @Test
    public void equalCoordinatesCompareEqual() {
        assertEquals(0, comparator.compare(new ChunkCoordIntPair(-4, 9), new ChunkCoordIntPair(-4, 9)));
    }

    @Test
    public void treeMapFindsEveryInsertedKeyRegardlessOfOrder() {
        List<ChunkCoordIntPair> pairs = new ArrayList<ChunkCoordIntPair>();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                pairs.add(new ChunkCoordIntPair(x, z));
            }
        }
        Collections.shuffle(pairs, new Random(1234567L));

        TreeMap<ChunkCoordIntPair, String> map = new TreeMap<ChunkCoordIntPair, String>(comparator);
        for (ChunkCoordIntPair pair : pairs) {
            map.put(pair, pair.chunkXPos + ":" + pair.chunkZPos);
        }

        for (ChunkCoordIntPair pair : pairs) {
            assertEquals(pair.chunkXPos + ":" + pair.chunkZPos, map.get(pair));
        }
    }
}
