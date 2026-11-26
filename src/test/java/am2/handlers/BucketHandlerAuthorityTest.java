package am2.handlers;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.blocks.BlocksCommonProxy;
import am2.items.ItemAMBucket;
import am2.items.ItemsCommonProxy;

public class BucketHandlerAuthorityTest {

    private Object previousLiquid;
    private Item previousBucket;

    @Before
    public void installFixtures() throws Exception {
        previousLiquid = BlocksCommonProxy.liquidEssence;
        previousBucket = ItemsCommonProxy.itemAMBucket;
        set(BlocksCommonProxy.class, "liquidEssence", null);
        set(ItemsCommonProxy.class, "itemAMBucket", mock(ItemAMBucket.class));
    }

    @After
    public void restoreFixtures() throws Exception {
        set(BlocksCommonProxy.class, "liquidEssence", previousLiquid);
        set(ItemsCommonProxy.class, "itemAMBucket", previousBucket);
    }

    @Test
    public void clientPredictionDoesNotRemoveTheSourceBlock() {
        World world = mock(World.class);
        world.isRemote = true;
        when(world.getBlock(1, 2, 3)).thenReturn(BlocksCommonProxy.liquidEssence);
        when(world.getBlockMetadata(1, 2, 3)).thenReturn(0);

        assertNotNull(BucketHandler.attemptFill(world, target()));
        verify(world, never()).setBlock(1, 2, 3, Blocks.air);
    }

    @Test
    public void serverRemovesTheSourceBlock() {
        World world = mock(World.class);
        when(world.getBlock(1, 2, 3)).thenReturn(BlocksCommonProxy.liquidEssence);
        when(world.getBlockMetadata(1, 2, 3)).thenReturn(0);

        assertNotNull(BucketHandler.attemptFill(world, target()));
        verify(world).setBlock(1, 2, 3, Blocks.air);
    }

    private static MovingObjectPosition target() {
        MovingObjectPosition target = mock(MovingObjectPosition.class);
        target.blockX = 1;
        target.blockY = 2;
        target.blockZ = 3;
        return target;
    }

    private static void set(Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
