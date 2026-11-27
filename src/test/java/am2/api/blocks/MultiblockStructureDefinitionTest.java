package am2.api.blocks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;

import net.minecraft.block.Block;

import org.junit.Test;

/**
 * Pins the {@code replaceAllBlocksOfType} precedence fix: the old {@code &&}/{@code ||} expression replaced every block
 * in the group whenever the two-argument overload was used.
 */
public class MultiblockStructureDefinitionTest {

    @Test
    public void replacingByBlockLeavesOtherBlocksAlone() {
        Block original = mock(Block.class);
        Block other = mock(Block.class);
        Block replacement = mock(Block.class);

        MultiblockStructureDefinition definition = new MultiblockStructureDefinition("test");
        definition.addAllowedBlock(0, 0, 0, original, 1);
        definition.addAllowedBlock(1, 0, 0, other, 2);

        definition.mainGroup.replaceAllBlocksOfType(original, replacement);

        ArrayList<BlockDec> replaced = definition.getAllowedBlocksAt(new BlockCoord(0, 0, 0));
        ArrayList<BlockDec> untouched = definition.getAllowedBlocksAt(new BlockCoord(1, 0, 0));
        assertEquals(
            replacement,
            replaced.get(0)
                .getBlock());
        assertEquals(
            other,
            untouched.get(0)
                .getBlock());
    }

    @Test
    public void replacingByBlockAndMetaStillRespectsTheMeta() {
        Block original = mock(Block.class);
        Block replacement = mock(Block.class);

        MultiblockStructureDefinition definition = new MultiblockStructureDefinition("test");
        definition.addAllowedBlock(0, 0, 0, original, 1);
        definition.addAllowedBlock(1, 0, 0, original, 2);

        definition.mainGroup.replaceAllBlocksOfType(original, 1, replacement, 5);

        assertEquals(
            replacement,
            definition.getAllowedBlocksAt(new BlockCoord(0, 0, 0))
                .get(0)
                .getBlock());
        assertEquals(
            5,
            definition.getAllowedBlocksAt(new BlockCoord(0, 0, 0))
                .get(0)
                .getMeta());
        assertEquals(
            original,
            definition.getAllowedBlocksAt(new BlockCoord(1, 0, 0))
                .get(0)
                .getBlock());
        assertEquals(
            2,
            definition.getAllowedBlocksAt(new BlockCoord(1, 0, 0))
                .get(0)
                .getMeta());
    }
}
