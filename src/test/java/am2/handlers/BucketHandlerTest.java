package am2.handlers;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.junit.Test;

public class BucketHandlerTest {

    @Test
    public void unrelatedBlockIsIgnored() {
        World world = mock(World.class);
        MovingObjectPosition target = mock(MovingObjectPosition.class);
        target.blockX = 1;
        target.blockY = 2;
        target.blockZ = 3;
        when(world.getBlock(1, 2, 3)).thenReturn(mock(Block.class));

        assertNull(BucketHandler.attemptFill(world, target));
    }
}
