package am2.worldgen;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Random;

import net.minecraft.world.World;

import org.junit.Test;

/**
 * `WitchwoodTreeHuge` used to keep `heightLimit` between calls, so reusing one generator would make every tree after
 * the first grow to the first tree's height. The world decorator now reuses the generator, so generation must reset it.
 */
public class WitchwoodTreeHugeReuseTest {

    @Test
    public void generationRerollsAStaleHeight() {
        ProbeTree tree = new ProbeTree();
        tree.heightLimit = 999;

        tree.generate(mock(World.class), new Random(1), 0, 64, 0);

        assertNotEquals(999, tree.heightLimit);
        assertNotEquals(999, tree.observedHeight);
        assertTrue(tree.observedHeight >= 5);
        assertTrue(tree.observedHeight < 19);
    }

    @Test
    public void aSecondGenerationAlsoRerolls() {
        ProbeTree tree = new ProbeTree();
        tree.generate(mock(World.class), new Random(1), 0, 64, 0);
        int first = tree.observedHeight;

        tree.heightLimit = first + 100;
        tree.generate(mock(World.class), new Random(2), 0, 64, 0);

        assertNotEquals(first + 100, tree.observedHeight);
        assertTrue(tree.observedHeight >= 5);
        assertTrue(tree.observedHeight < 19);
    }

    private static final class ProbeTree extends WitchwoodTreeHuge {

        int observedHeight = -1;

        private ProbeTree() {
            super(true);
        }

        @Override
        boolean validTreeLocation() {
            observedHeight = heightLimit;
            return false;
        }
    }
}
